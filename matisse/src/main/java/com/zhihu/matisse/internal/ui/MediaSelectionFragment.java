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
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.FillModeCustomItem;
import com.daasuu.mp4compose.composer.Mp4Composer;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropFragment;
import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.AlbumMediaCollection;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter;
import com.zhihu.matisse.internal.ui.widget.MediaGridInset;
import com.zhihu.matisse.internal.utils.PathUtils;
import com.zhihu.matisse.internal.utils.Utils;
import com.zhihu.matisse.ui.MatisseActivity;
import com.zhihu.matisse.ui.widget.GesturePlayerTextureView;
import com.zhihu.matisse.ui.widget.SceneCropColor;

import java.io.File;
import java.util.Calendar;

import static com.yalantis.ucrop.UCropFragment.TAG;

public class MediaSelectionFragment extends Fragment implements
        AlbumMediaCollection.AlbumMediaCallbacks, AlbumMediaAdapter.CheckStateListener,
        AlbumMediaAdapter.OnMediaClickListener {

    public static final String EXTRA_ALBUM = "extra_album";

    private final AlbumMediaCollection mAlbumMediaCollection = new AlbumMediaCollection();
    private RecyclerView mRecyclerView;
    private GesturePlayerTextureView playerTextureView;
    private AlbumMediaAdapter mAdapter;
    private SelectionProvider mSelectionProvider;
    private AlbumMediaAdapter.CheckStateListener mCheckStateListener;
    private AlbumMediaAdapter.OnMediaClickListener mOnMediaClickListener;
    public UCropFragment fragment;
    private Uri destinationUri;
    private Album album;
    private boolean isFirst = true;
    private Context context;

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
        this.context = context;
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
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        album = getArguments().getParcelable(EXTRA_ALBUM);

        mAdapter = new AlbumMediaAdapter(context,
                mSelectionProvider.provideSelectedItemCollection(), mRecyclerView);
        mAdapter.registerCheckStateListener(this);
        mAdapter.registerOnMediaClickListener(this);
        mRecyclerView.setHasFixedSize(true);

        int spanCount;
        SelectionSpec selectionSpec = SelectionSpec.getInstance();
        if (selectionSpec.gridExpectedSize > 0) {
            spanCount = Utils.Companion.spanCount(context, selectionSpec.gridExpectedSize);
        } else {
            spanCount = selectionSpec.spanCount;
        }
        mRecyclerView.setLayoutManager(new GridLayoutManager(context, spanCount));

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
            showPreviewItem(Item.valueOf(cursor));
//            SelectedItemCollection collection = mSelectionProvider.provideSelectedItemCollection();
//            if (collection.isEmpty()) collection.add(Item.valueOf(cursor));
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
    public void onMediaClick(Album album, Item item, int adapterPosition) {
        if (mOnMediaClickListener != null) {
            mOnMediaClickListener.onMediaClick(getArguments().getParcelable(EXTRA_ALBUM),
                    item, adapterPosition);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (playerTextureView != null) {
            playerTextureView.play();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (playerTextureView != null) {
            playerTextureView.pause();
        }
    }

    @Override
    public void onMediaAdded(Item item, Item prev) {
        cropItem(prev);

        showPreviewItem(item);
    }

    public boolean onNextButtonClick() {
        return cropItem(mAdapter.mPrevious);
    }

    private boolean cropItem(Item item) {
        if (!mSelectionProvider.provideSelectedItemCollection().isSelected(item)) return true;

        if (item.isImage()) return cropImage(item);
        else return cropVideo(item);
    }

    private boolean cropVideo(Item item) {
        MatisseActivity activity = (MatisseActivity) getActivity();
        activity.showProgress();
        File file = getFile(false);
        destinationUri = Uri.fromFile(file);
        if (destinationUri.getPath() == null) return true;

        String path = PathUtils.getPath(context, item.getContentUri());
        FillModeCustomItem fillModeCustomItem = Utils.Companion.getFillMode(playerTextureView, path);

        new Mp4Composer(path, file.getPath())
                .size(720, 720)
                .filter(Utils.Companion.getFill(SceneCropColor.WHITE))
                .fillMode(FillMode.CUSTOM)
                .customFillMode(fillModeCustomItem)
                .listener(activity)
                .start();

        item.uri = destinationUri;
        return false;
    }

    private boolean cropImage(Item item) {
        // crop and save Current image
        if (fragment != null && fragment.isAdded()) {
            fragment.cropAndSaveImage();
            item.uri = destinationUri;
            Log.d("cropAndSaveImage: ", destinationUri.toString());
            return false;
        }
        return true;
    }


    private void showPreviewItem(Item item) {
        if (item.isImage()) showPreviewImage(item.uri);
        else showPreviewVideo(item);
    }

    private void showPreviewVideo(Item item) {
        FrameLayout parent = getView().findViewById(R.id.mPreview);
        parent.removeAllViews();
        playerTextureView = new GesturePlayerTextureView(context, item.uri, null);

        Point size = new Point();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(size);
        float baseWidthSize = size.x;
        playerTextureView.setBaseWidthSize(baseWidthSize);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        playerTextureView.setLayoutParams(lp);

        parent.addView(playerTextureView);
    }

    private void showPreviewImage(Uri uri) {
        FrameLayout parent = getView().findViewById(R.id.mPreview);
        parent.removeAllViews();

        destinationUri = Uri.fromFile(getFile(true));
        UCrop uCrop = UCrop.of(uri, destinationUri);
        uCrop = setupConfig(uCrop);

        if (getActivity() == null) return;
        fragment = uCrop.getFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.mPreview, fragment, TAG)
                .commitAllowingStateLoss();
    }

    private File getFile(boolean isImage) {
        String name = String.valueOf(Calendar.getInstance().getTimeInMillis());
        if (isImage) name += ".jpeg";
        else name += ".mp4";
        File file = new File(new ContextWrapper(context).getCacheDir(), name);
        Log.d("getFile: ", file.getPath());
        return file;
    }

    private UCrop setupConfig(UCrop uCrop) {
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCropFrameStrokeWidth(5);
        options.setCompressionQuality(70);
        options.setHideBottomControls(true);

        uCrop = uCrop.withAspectRatio(1, 1);
        uCrop = uCrop.withMaxResultSize(SelectionSpec.getInstance().cropMaxSize,
                SelectionSpec.getInstance().cropMaxSize);
        return uCrop.withOptions(options);
    }

    public interface SelectionProvider {
        SelectedItemCollection provideSelectedItemCollection();
    }
}
