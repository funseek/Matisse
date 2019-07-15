/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropFragment;
import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.AlbumMediaCollection;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter;
import com.zhihu.matisse.internal.ui.widget.CheckView;
import com.zhihu.matisse.internal.ui.widget.MediaGridInset;
import com.zhihu.matisse.internal.utils.UIUtils;

import java.io.File;
import java.util.Calendar;

import static com.yalantis.ucrop.UCropFragment.TAG;

public class MediaSelectionFragment extends Fragment implements
        AlbumMediaCollection.AlbumMediaCallbacks, AlbumMediaAdapter.CheckStateListener,
        AlbumMediaAdapter.OnMediaClickListener {

    public static final String EXTRA_ALBUM = "extra_album";

    private final AlbumMediaCollection mAlbumMediaCollection = new AlbumMediaCollection();
    private RecyclerView mRecyclerView;
    private AlbumMediaAdapter mAdapter;
    private SelectionProvider mSelectionProvider;
    private AlbumMediaAdapter.CheckStateListener mCheckStateListener;
    private AlbumMediaAdapter.OnMediaClickListener mOnMediaClickListener;
    private static final int CROP_SIZE = 2000; // max crop px
    private AppBarLayout mAppBarLayout;
    //    private CropImageView mCropImageView;
    public UCropFragment fragment;
    private Uri destinationUri;
    private Album album;
    private boolean isFirst = true;

    public static MediaSelectionFragment newInstance(Album album) {
        MediaSelectionFragment fragment = new MediaSelectionFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ALBUM, album);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SelectionProvider) {
            mSelectionProvider = (SelectionProvider) context;
        } else {
            throw new IllegalStateException("Context must implement SelectionProvider.");
        }
        if (context instanceof AlbumMediaAdapter.CheckStateListener) {
            mCheckStateListener = (AlbumMediaAdapter.CheckStateListener) context;
        }
        if (context instanceof AlbumMediaAdapter.OnMediaClickListener) {
            mOnMediaClickListener = (AlbumMediaAdapter.OnMediaClickListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_selection, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = view.findViewById(R.id.recyclerview);
        mAppBarLayout = view.findViewById(R.id.mAppBarContainer);
//        mCropImageView = view.findViewById(R.id.mPreview);
//        mCropImageView.setMaxCropResultSize(CROP_SIZE, CROP_SIZE);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        album = getArguments().getParcelable(EXTRA_ALBUM);

        mAdapter = new AlbumMediaAdapter(getContext(),
                mSelectionProvider.provideSelectedItemCollection(), mRecyclerView);
        mAdapter.registerCheckStateListener(this);
        mAdapter.registerOnMediaClickListener(this);
        mRecyclerView.setHasFixedSize(true);

        int spanCount;
        SelectionSpec selectionSpec = SelectionSpec.getInstance();
        if (selectionSpec.gridExpectedSize > 0) {
            spanCount = UIUtils.spanCount(getContext(), selectionSpec.gridExpectedSize);
        } else {
            spanCount = selectionSpec.spanCount;
        }
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));

        int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_spacing);
        mRecyclerView.addItemDecoration(new MediaGridInset(spanCount, spacing, false));
        mRecyclerView.setAdapter(mAdapter);
        mAlbumMediaCollection.onCreate(getActivity(), this);
        mAlbumMediaCollection.load(album, selectionSpec.capture);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAlbumMediaCollection.onDestroy();
    }

    public void refreshMediaGrid() {
        mAdapter.notifyDataSetChanged();
    }

    public void refreshSelection() {
        mAdapter.refreshSelection();
    }

    @Override
    public void onAlbumMediaLoad(Cursor cursor) {
        mAdapter.swapCursor(cursor);
        if (isFirst) {
            isFirst = false;
            cursor.moveToPosition(album.isAll() ? 1 : 0);
            showPreviewImage(Item.valueOf(cursor).uri);
        }
    }

    @Override
    public void onAlbumMediaReset() {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onUpdate() {
        // notify outer Activity that check state changed
        if (mCheckStateListener != null) {
            mCheckStateListener.onUpdate();
        }
    }

    @Override
    public void onMediaClick(Album album, Item item, Item mPrevious, int adapterPosition) {
        if (!item.equals(mPrevious)) {
            cropCurrentImage(mPrevious);
        }

        if (!item.equals(mPrevious))
            showPreviewImage(item.uri);
    }

    public boolean onNextButtonClick() {
        return cropCurrentImage(mAdapter.mPrevious);
    }

    private boolean cropCurrentImage(Item item) {
        // crop and save Current image
        SelectedItemCollection collection = mSelectionProvider.provideSelectedItemCollection();
        int checkedNum = collection.checkedNumOf(item);
        if (fragment != null && fragment.isAdded() && checkedNum != CheckView.UNCHECKED) {
            fragment.cropAndSaveImage();
            item.uriCrop = destinationUri;
            Log.d("cropAndSaveImage: ", destinationUri.toString());
            return false;
        }
        return true;
    }


    public void showPreviewImage(Uri uri) {
        // load new image
        String destinationFileName = String.format("%s.jpeg", Calendar.getInstance().getTimeInMillis());

        destinationUri = Uri.fromFile(new File(new ContextWrapper(getContext()).getCacheDir(), destinationFileName));
        UCrop uCrop = UCrop.of(uri, destinationUri);
        uCrop = setupConfig(uCrop);

        if (getActivity() == null) return;
        fragment = uCrop.getFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.mPreview, fragment, TAG)
                .commitAllowingStateLoss();
    }


    private UCrop setupConfig(UCrop uCrop) {
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCropFrameStrokeWidth(5);
        options.setCompressionQuality(70);
        options.setHideBottomControls(true);

        uCrop = uCrop.withAspectRatio(1, 1);
        uCrop = uCrop.withMaxResultSize(CROP_SIZE, CROP_SIZE);
        return uCrop.withOptions(options);
    }

    public interface SelectionProvider {
        SelectedItemCollection provideSelectedItemCollection();
    }
}
