/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package android.idigisign.com.advancerecycleview;

import android.graphics.drawable.NinePatchDrawable;
import android.idigisign.com.advancerecycleview.common.data.AbstractExpandableDataProvider;
import android.idigisign.com.advancerecycleview.common.fragment.ExampleExpandableDataProviderFragment;
import android.idigisign.com.advancerecycleview.common.fragment.ExpandableItemPinnedMessageDialogFragment;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;

import java.util.ArrayList;

public class RecyclerListViewFragment extends Fragment
    implements RecyclerViewExpandableItemManager.OnGroupCollapseListener,
        RecyclerViewExpandableItemManager.OnGroupExpandListener,ExpandableItemPinnedMessageDialogFragment.EventListener{
    private static final String SAVED_STATE_EXPANDABLE_ITEM_MANAGER = "RecyclerViewExpandableItemManager";

    //define the recycler view object
    private RecyclerView mRecyclerView;
    //define the layoutManager
    private RecyclerView.LayoutManager mLayoutManager;
    //define the data adapter
    private RecyclerView.Adapter mAdapter;
    //?
    private RecyclerView.Adapter mWrappedAdapter;

    //define the advance function manager
    private RecyclerViewExpandableItemManager mRecyclerViewExpandableItemManager;
    private RecyclerViewDragDropManager mRecyclerViewDragDropManager;
    private RecyclerViewSwipeManager mRecyclerViewSwipeManager;
    private RecyclerViewTouchActionGuardManager mRecyclerViewTouchActionGuardManager;

    private static final String FRAGMENT_TAG_DATA_PROVIDER = "data provider";
    private static final String FRAGMENT_LIST_VIEW = "list view";
    private static final String FRAGMENT_TAG_ITEM_PINNED_DIALOG = "item pinned dialog";

    //define the data provider
    private AbstractExpandableDataProvider dataProvider;

    public RecyclerListViewFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recycler_list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataProvider = getDataProvider();

        //get the instance of recycler view and the LinearLayoutManager
        //noinspection ConstantConditions
        mRecyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(getContext());

        final Parcelable eimSavedState = (savedInstanceState != null) ? savedInstanceState.getParcelable(SAVED_STATE_EXPANDABLE_ITEM_MANAGER) : null;
        mRecyclerViewExpandableItemManager = new RecyclerViewExpandableItemManager(eimSavedState);
        mRecyclerViewExpandableItemManager.setOnGroupExpandListener(this);
        mRecyclerViewExpandableItemManager.setOnGroupCollapseListener(this);

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mRecyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mRecyclerViewTouchActionGuardManager.setEnabled(true);

        // drag & drop manager
        mRecyclerViewDragDropManager = new RecyclerViewDragDropManager();
        mRecyclerViewDragDropManager.setDraggingItemShadowDrawable(
                (NinePatchDrawable) ContextCompat.getDrawable(getContext(), R.drawable.material_shadow_z3));

        // swipe manager
        mRecyclerViewSwipeManager = new RecyclerViewSwipeManager();

        //item adapter
        final MyExpandableDraggableSwipeableItemAdapter myItemAdapter =
                new MyExpandableDraggableSwipeableItemAdapter(mRecyclerViewExpandableItemManager, dataProvider);

        myItemAdapter.setEventListener(new MyExpandableDraggableSwipeableItemAdapter.EventListener() {
            @Override
            public void onGroupItemRemoved(int groupPosition) {
                onGroupItemRemovedAction(groupPosition);
            }

            @Override
            public void onChildItemRemoved(int groupPosition, int childPosition) {
                onChildItemRemovedAction(groupPosition, childPosition);
            }

            @Override
            public void onGroupItemPinned(int groupPosition) {
                onGroupItemPinnedAction(groupPosition);
            }

            @Override
            public void onChildItemPinned(int groupPosition, int childPosition) {
                onChildItemPinnedAction(groupPosition, childPosition);
            }

            @Override
            public void onItemViewClicked(View v, boolean pinned) {
                onItemViewClick(v, pinned);
            }
        });

        mAdapter = myItemAdapter;

        //create the wrap adapter base on the basic adapter, according to different Manager,
        // add different interface to the adapter.
        // wrap for expanding
        mWrappedAdapter = mRecyclerViewExpandableItemManager.createWrappedAdapter(myItemAdapter);
        // wrap for dragging
        mWrappedAdapter = mRecyclerViewDragDropManager.createWrappedAdapter(mWrappedAdapter);
        // wrap for swiping
        mWrappedAdapter = mRecyclerViewSwipeManager.createWrappedAdapter(mWrappedAdapter);

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        // Also need to disable them when using animation indicator.
        animator.setSupportsChangeAnimations(false);

        //configure the RecyclerView
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mWrappedAdapter);  // requires *wrapped* adapter
        mRecyclerView.setItemAnimator(animator);
        mRecyclerView.setHasFixedSize(false);

        // additional decorations
        //noinspection StatementWithEmptyBody
        if (supportsViewElevation()) {
            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
        } else {
            mRecyclerView.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) ContextCompat.getDrawable(getContext(), R.drawable.material_shadow_z1)));
        }
        mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(ContextCompat.getDrawable(getContext(), R.drawable.list_divider_h), true));


        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop > ExpandableItem
        mRecyclerViewTouchActionGuardManager.attachRecyclerView(mRecyclerView);
        mRecyclerViewSwipeManager.attachRecyclerView(mRecyclerView);
        mRecyclerViewDragDropManager.attachRecyclerView(mRecyclerView);
        mRecyclerViewExpandableItemManager.attachRecyclerView(mRecyclerView);


        //ADD action for fab to add a item to the list
        FloatingActionButton fabBtn = (FloatingActionButton) getActivity().findViewById(R.id.fab_add);
        fabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Add a item to the list
                ArrayList<String> childTextList = new ArrayList<String>();

                childTextList.add("test child 1");
                childTextList.add("test child 2");
                dataProvider.addItem("TestAdd",childTextList);
                myItemAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save current state to support screen rotation, etc...
        if (mRecyclerViewExpandableItemManager != null) {
            outState.putParcelable(
                    SAVED_STATE_EXPANDABLE_ITEM_MANAGER,
                    mRecyclerViewExpandableItemManager.getSavedState());
        }
    }

    @Override
    public void onDestroyView() {
        if (mRecyclerViewDragDropManager != null) {
            mRecyclerViewDragDropManager.release();
            mRecyclerViewDragDropManager = null;
        }

        if (mRecyclerViewSwipeManager != null) {
            mRecyclerViewSwipeManager.release();
            mRecyclerViewSwipeManager = null;
        }

        if (mRecyclerViewTouchActionGuardManager != null) {
            mRecyclerViewTouchActionGuardManager.release();
            mRecyclerViewTouchActionGuardManager = null;
        }

        if (mRecyclerViewExpandableItemManager != null) {
            mRecyclerViewExpandableItemManager.release();
            mRecyclerViewExpandableItemManager = null;
        }

        if (mRecyclerView != null) {
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.setAdapter(null);
            mRecyclerView = null;
        }

        if (mWrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(mWrappedAdapter);
            mWrappedAdapter = null;
        }
        mAdapter = null;
        mLayoutManager = null;

        super.onDestroyView();
    }

    @Override
    public void onGroupCollapse(int groupPosition, boolean fromUser) {
    }

    @Override
    public void onGroupExpand(int groupPosition, boolean fromUser) {
        if (fromUser) {
            adjustScrollPositionOnGroupExpanded(groupPosition);
        }
    }

    private void adjustScrollPositionOnGroupExpanded(int groupPosition) {
        int childItemHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.list_item_height);
        int topMargin = (int) (getActivity().getResources().getDisplayMetrics().density * 16); // top-spacing: 16dp
        int bottomMargin = topMargin; // bottom-spacing: 16dp

        mRecyclerViewExpandableItemManager.scrollToGroup(groupPosition, childItemHeight, topMargin, bottomMargin);
    }

    private void onItemViewClick(View v, boolean pinned) {
        final int flatPosition = mRecyclerView.getChildAdapterPosition(v);

        if (flatPosition == RecyclerView.NO_POSITION) {
            return;
        }

        final long expandablePosition = mRecyclerViewExpandableItemManager.getExpandablePosition(flatPosition);
        final int groupPosition = RecyclerViewExpandableItemManager.getPackedPositionGroup(expandablePosition);
        final int childPosition = RecyclerViewExpandableItemManager.getPackedPositionChild(expandablePosition);

        if (childPosition == RecyclerView.NO_POSITION) {
            onGroupItemClickedAction(groupPosition);
        } else {
            onChildItemClickedAction(groupPosition, childPosition);
        }
    }

    private boolean supportsViewElevation() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }

//    public AbstractExpandableDataProvider getDataProvider() {
//        return ((MainActivity) getActivity()).getDataProvider();
//    }

    public void notifyGroupItemRestored(int groupPosition) {
        mAdapter.notifyDataSetChanged();

        final long expandablePosition = RecyclerViewExpandableItemManager.getPackedPositionForGroup(groupPosition);
        final int flatPosition = mRecyclerViewExpandableItemManager.getFlatPosition(expandablePosition);
        mRecyclerView.scrollToPosition(flatPosition);
    }

    public void notifyChildItemRestored(int groupPosition, int childPosition) {
        mAdapter.notifyDataSetChanged();

        final long expandablePosition = RecyclerViewExpandableItemManager.getPackedPositionForChild(groupPosition, childPosition);
        final int flatPosition = mRecyclerViewExpandableItemManager.getFlatPosition(expandablePosition);
        mRecyclerView.scrollToPosition(flatPosition);
    }

    public void notifyGroupItemChanged(int groupPosition) {
        final long expandablePosition = RecyclerViewExpandableItemManager.getPackedPositionForGroup(groupPosition);
        final int flatPosition = mRecyclerViewExpandableItemManager.getFlatPosition(expandablePosition);

        mAdapter.notifyItemChanged(flatPosition);
    }

    public void notifyChildItemChanged(int groupPosition, int childPosition) {
        final long expandablePosition = RecyclerViewExpandableItemManager.getPackedPositionForChild(groupPosition, childPosition);
        final int flatPosition = mRecyclerViewExpandableItemManager.getFlatPosition(expandablePosition);

        mAdapter.notifyItemChanged(flatPosition);
    }

    @Override
    public void onNotifyExpandableItemPinnedDialogDismissed(int groupPosition, int childPosition, boolean ok) {
        final Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VIEW);

        if (childPosition == RecyclerView.NO_POSITION) {
            // group item
            dataProvider.getGroupItem(groupPosition).setPinned(ok);
            ((RecyclerListViewFragment) fragment).notifyGroupItemChanged(groupPosition);
        } else {
            // child item
            dataProvider.getChildItem(groupPosition, childPosition).setPinned(ok);
            ((RecyclerListViewFragment) fragment).notifyChildItemChanged(groupPosition, childPosition);
        }
    }


    public AbstractExpandableDataProvider getDataProvider() {
        final Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DATA_PROVIDER);
        return ((ExampleExpandableDataProviderFragment) fragment).getDataProvider();
    }

    /**
     * This method will be called when a group item is removed
     *
     * @param groupPosition The position of the group item within data set
     */
    public void onGroupItemRemovedAction(int groupPosition) {
        Snackbar snackbar = Snackbar.make(
                getActivity().findViewById(R.id.container),
                R.string.snack_bar_text_group_item_removed,
                Snackbar.LENGTH_LONG);

        snackbar.setAction(R.string.snack_bar_action_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemUndoActionClickedAction();
            }
        });
        snackbar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.snackbar_action_color_done));
        snackbar.show();
    }

    /**
     * This method will be called when a child item is removed
     *
     * @param groupPosition The group position of the child item within data set
     * @param childPosition The position of the child item within the group
     */
    public void onChildItemRemovedAction(int groupPosition, int childPosition) {
        Snackbar snackbar = Snackbar.make(
                getActivity().findViewById(R.id.container),
                R.string.snack_bar_text_child_item_removed,
                Snackbar.LENGTH_LONG);

        snackbar.setAction(R.string.snack_bar_action_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemUndoActionClickedAction();
            }
        });
        snackbar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.snackbar_action_color_done));
        snackbar.show();
    }

    /**
     * This method will be called when a group item is pinned
     *
     * @param groupPosition The position of the group item within data set
     */
    public void onGroupItemPinnedAction(int groupPosition) {
        final DialogFragment dialog = ExpandableItemPinnedMessageDialogFragment.newInstance(groupPosition, RecyclerView.NO_POSITION);
        dialog.setTargetFragment(this,0);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(dialog, FRAGMENT_TAG_ITEM_PINNED_DIALOG)
                .commit();
    }

    /**
     * This method will be called when a child item is pinned
     *
     * @param groupPosition The group position of the child item within data set
     * @param childPosition The position of the child item within the group
     */
    public void onChildItemPinnedAction(int groupPosition, int childPosition) {
        final DialogFragment dialog = ExpandableItemPinnedMessageDialogFragment.newInstance(groupPosition, childPosition);

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(dialog, FRAGMENT_TAG_ITEM_PINNED_DIALOG)
                .commit();
    }

    public void onGroupItemClickedAction(int groupPosition) {
        final Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VIEW);
        AbstractExpandableDataProvider.GroupData data = dataProvider.getGroupItem(groupPosition);

        if (data.isPinned()) {
            // unpin if tapped the pinned item
            data.setPinned(false);
            ((RecyclerListViewFragment) fragment).notifyGroupItemChanged(groupPosition);
        }
    }

    public void onChildItemClickedAction(int groupPosition, int childPosition) {
        final Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VIEW);
        AbstractExpandableDataProvider.ChildData data = dataProvider.getChildItem(groupPosition, childPosition);

        if (data.isPinned()) {
            // unpin if tapped the pinned item
            data.setPinned(false);
            ((RecyclerListViewFragment) fragment).notifyChildItemChanged(groupPosition, childPosition);
        }
    }

    private void onItemUndoActionClickedAction() {
        final Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VIEW);
        final long result = dataProvider.undoLastRemoval();

        if (result == RecyclerViewExpandableItemManager.NO_EXPANDABLE_POSITION) {
            return;
        }

        final int groupPosition = RecyclerViewExpandableItemManager.getPackedPositionGroup(result);
        final int childPosition = RecyclerViewExpandableItemManager.getPackedPositionChild(result);

        if (childPosition == RecyclerView.NO_POSITION) {
            // group item
            ((RecyclerListViewFragment) fragment).notifyGroupItemRestored(groupPosition);
        } else {
            // child item
            ((RecyclerListViewFragment) fragment).notifyChildItemRestored(groupPosition, childPosition);
        }
    }
}
