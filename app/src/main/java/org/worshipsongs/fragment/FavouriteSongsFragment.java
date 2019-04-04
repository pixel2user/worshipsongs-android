package org.worshipsongs.fragment;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragListView;

import org.apache.commons.lang3.StringUtils;
import org.worshipsongs.CommonConstants;
import org.worshipsongs.R;
import org.worshipsongs.WorshipSongApplication;
import org.worshipsongs.activity.SongContentViewActivity;
import org.worshipsongs.adapter.FavouriteSongAdapter;
import org.worshipsongs.domain.Favourite;
import org.worshipsongs.domain.Setting;
import org.worshipsongs.domain.Song;
import org.worshipsongs.domain.SongDragDrop;
import org.worshipsongs.listener.SongContentViewListener;
import org.worshipsongs.service.FavouriteService;
import org.worshipsongs.service.PopupMenuService;
import org.worshipsongs.service.SongService;
import org.worshipsongs.service.UserPreferenceSettingService;
import org.worshipsongs.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Author : Madasamy
 * Version : 0.1.0
 */

public class FavouriteSongsFragment extends Fragment implements FavouriteSongAdapter.FavouriteListener
{
    private List<SongDragDrop> configureDragDrops;
    private FavouriteSongAdapter favouriteSongAdapter;

    private SongContentViewListener songContentViewListener;
    private FavouriteService favouriteService;
    private SongService songService;
    private PopupMenuService popupMenuService = new PopupMenuService();
    private UserPreferenceSettingService userPreferenceSettingService = new UserPreferenceSettingService();

    public static FavouriteSongsFragment newInstance(Bundle bundle)
    {
        FavouriteSongsFragment favouriteSongsFragment = new FavouriteSongsFragment();
        favouriteSongsFragment.setArguments(bundle);
        return favouriteSongsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        favouriteService = new FavouriteService();
        songService = new SongService(getContext());
        String serviceName = getArguments().getString(CommonConstants.SERVICE_NAME_KEY);
        Favourite favourite = favouriteService.find(serviceName);
        configureDragDrops = favourite.getDragDrops();
        setHasOptionsMenu(true);
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.favourite_song_layout, container, false);
        setDragListView(view);
        return view;
    }

    private void setDragListView(View view)
    {
        DragListView dragListView = (DragListView) view.findViewById(R.id.drag_list_view);
        dragListView.getRecyclerView().setVerticalScrollBarEnabled(true);
        dragListView.setLayoutManager(new LinearLayoutManager(getContext()));
        favouriteSongAdapter = new FavouriteSongAdapter(configureDragDrops);
        favouriteSongAdapter.setFavouriteListener(this);
        dragListView.setAdapter(favouriteSongAdapter, true);
        dragListView.setCanDragHorizontally(false);
        dragListView.setCustomDragItem(new MyDragItem(getContext(), R.layout.favourite_song_adapter));
        dragListView.setDragListListener(new FavouriteDragListListener());
    }

    @Override
    public void onRemove(final SongDragDrop dragDrop)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.MyDialogTheme));
        builder.setTitle(getString(R.string.remove_favourite_song_title));
        builder.setMessage(getString(R.string.remove_favourite_song_message));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                favouriteService.removeSong(getArguments().getString(CommonConstants.SERVICE_NAME_KEY), dragDrop.getTitle());
                configureDragDrops.remove(dragDrop);
                favouriteSongAdapter.setItemList(configureDragDrops);
                favouriteSongAdapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onClick(SongDragDrop dragDrop)
    {
        Song song = songService.findContentsByTitle(dragDrop.getTitle());
        if (song != null) {
            ArrayList<String> titles = new ArrayList<>();
            titles.add(dragDrop.getTitle());
            if (CommonUtils.isPhone(getContext())) {
                Intent intent = new Intent(getActivity(), SongContentViewActivity.class);
                Bundle bundle = new Bundle();
                bundle.putStringArrayList(CommonConstants.TITLE_LIST_KEY, titles);
                bundle.putInt(CommonConstants.POSITION_KEY, 0);
                Setting.getInstance().setPosition(0);
                intent.putExtras(bundle);
                getActivity().startActivity(intent);
            } else {
                Setting.getInstance().setPosition(favouriteSongAdapter.getPositionForItem(dragDrop));
                songContentViewListener.displayContent(dragDrop.getTitle(), titles,
                        favouriteSongAdapter.getPositionForItem(dragDrop));
            }
        } else {
            Bundle bundle = new Bundle();
            bundle.putString(CommonConstants.TITLE_KEY, getString(R.string.warning));
            bundle.putString(CommonConstants.MESSAGE_KEY, getString(R.string.message_song_not_available,
                    "\"" + getTitle(dragDrop) + "\""));
            AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(bundle);
            alertDialogFragment.setVisibleNegativeButton(false);
            alertDialogFragment.show(getActivity().getFragmentManager(), "WarningDialogFragment");
        }
    }

    private String getTitle(SongDragDrop dragDrop)
    {
        return userPreferenceSettingService.isTamil() && StringUtils.isNotBlank(dragDrop.getTamilTitle())
                ? dragDrop.getTamilTitle() : dragDrop.getTitle();
    }

    private static class MyDragItem extends DragItem
    {
        MyDragItem(Context context, int layoutId)
        {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView)
        {
            CharSequence text = ((TextView) clickedView.findViewById(R.id.text)).getText();
            ((TextView) dragView.findViewById(R.id.text)).setText(text);
            //dragView.findViewById(R.id.item_layout).setBackgroundColor(dragView.getResources().getColor(R.color.list_item_background));
        }
    }

    private class FavouriteDragListListener implements DragListView.DragListListener
    {
        @Override
        public void onItemDragStarted(int position)
        {
            //Do nothing
        }

        @Override
        public void onItemDragging(int itemPosition, float x, float y)
        {
            //Do nothing
        }

        @Override
        public void onItemDragEnded(int fromPosition, int toPosition)
        {
            favouriteService.save(getArguments().getString(CommonConstants.SERVICE_NAME_KEY), favouriteSongAdapter.getItemList());
        }
    }

    public void setSongContentViewListener(SongContentViewListener songContentViewListener)
    {
        this.songContentViewListener = songContentViewListener;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        menu.clear();
        if (CommonUtils.isPhone(getContext())) {
            inflater.inflate(R.menu.action_bar_options, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        Log.i(FavouriteSongsFragment.class.getSimpleName(), "Menu item " +
                item.getItemId() + " " + R.id.options);
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
               return true;
            case R.id.options:
                popupMenuService.shareFavouritesInSocialMedia(getActivity(), getActivity().findViewById(R.id.options),
                        getArguments().getString(CommonConstants.SERVICE_NAME_KEY));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
