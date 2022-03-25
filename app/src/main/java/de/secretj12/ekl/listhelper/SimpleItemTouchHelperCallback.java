package de.secretj12.ekl.listhelper;

import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import de.secretj12.ekl.R;

public class SimpleItemTouchHelperCallback
        extends ItemTouchHelper.Callback {
    private final ItemTouchHelperAdapter adapter;

    public SimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                (((ItemViewHolder) viewHolder).isEnableAble() ? ItemTouchHelper.LEFT : 0)
                        | (((ItemViewHolder) viewHolder).isCancelAble() ? ItemTouchHelper.RIGHT : 0));
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
        return (current.getItemViewType() == 1 && target.getItemViewType() == 1)
                || (target.getAdapterPosition() != 0 && current.getItemViewType() == 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        if (direction == ItemTouchHelper.RIGHT) {
            adapter.onItemCancel(viewHolder.getAdapterPosition(), (ItemViewHolder) viewHolder);
            ((ItemViewHolder) viewHolder).setSwiped();
            viewHolder.itemView.findViewById(R.id.swipeContent).animate().translationX(0)
                    .withEndAction(() -> {
                        adapter.onItemUpdated(viewHolder.getAdapterPosition(), (ItemViewHolder) viewHolder);
                        viewHolder.itemView.findViewById(R.id.swipeContent).setTranslationX(0);
                    }).start();
        } else {
            adapter.onItemEnable(viewHolder.getAdapterPosition(), (ItemViewHolder) viewHolder);
            ((ItemViewHolder) viewHolder).setSwiped();
            viewHolder.itemView.findViewById(R.id.swipeContent).animate().translationX(0)
                    .withEndAction(() -> {
                        adapter.onItemUpdated(viewHolder.getAdapterPosition(), (ItemViewHolder) viewHolder);
                        viewHolder.itemView.findViewById(R.id.swipeContent).setTranslationX(0);
                    }).start();
        }
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE
                && ItemTouchHelper.ACTION_STATE_DRAG == actionState
                && viewHolder instanceof ItemTouchHelperViewHolder) {
            ((ItemTouchHelperViewHolder) viewHolder).onItemSelected();
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        adapter.onActionEnded();
        if (viewHolder instanceof ItemTouchHelperViewHolder) {
            ((ItemTouchHelperViewHolder) viewHolder).onItemClear();
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (((ItemViewHolder) viewHolder).isSwiped())
                return;

            if (dX < 0)
                ((ItemViewHolder) viewHolder).onItemSwipeCancel();
            else
                ((ItemViewHolder) viewHolder).onItemSwipeEnable();

            int width = viewHolder.itemView.getWidth();
            if (dX > 0) {
                if (dX < width * 0.1)
                    viewHolder.itemView.findViewById(R.id.swipeContent).setTranslationX(dX);
                else
                    viewHolder.itemView.findViewById(R.id.swipeContent).setTranslationX((float) (width * 0.1
                            + slowSwipeFormula(width, dX)));
            } else {
                if (dX > -width * 0.1)
                    viewHolder.itemView.findViewById(R.id.swipeContent).setTranslationX(dX);
                else
                    viewHolder.itemView.findViewById(R.id.swipeContent).setTranslationX((float) (-width * 0.1
                            - slowSwipeFormula(width, -dX)));
            }
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    private float slowSwipeFormula(int width, float dX) {
        return (float) ((dX - (width * 0.1))
                * (1 / Math.pow((1f / 2 * ((dX - (width * 0.1)) / (width * 0.9) * 5) + 1), 2)));
    }
}
