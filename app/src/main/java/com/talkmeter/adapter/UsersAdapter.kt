package com.talkmeter.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.talkmeter.MainActivity
import com.talkmeter.R
import com.talkmeter.databinding.ItRecyclerHeaderBinding
import com.talkmeter.databinding.ItRecyclerItemBinding
import com.talkmeter.db.User

class UsersAdapter(val context: Context?, val userCategory: Int = 1) :
    ListAdapter<User, UsersAdapter.CustomViewHolder>(UsersDiffCallback()) {

    companion object {
        val TYPE_HEADER = 0
        val TYPE_ITEM = 1
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        if (viewType == TYPE_HEADER) {
            return HeaderGalleryViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.it_recycler_header, parent,
                    false
                )
            )
        } else if (viewType == TYPE_ITEM) {
            return UserViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.it_recycler_item, parent,
                    false
                )
            )
        }
        throw RuntimeException("No view type matches error")
    }

    override fun getItemViewType(position: Int): Int {
        if (position == itemCount - 1) {
            return TYPE_HEADER
        }
        return TYPE_ITEM
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        if (holder is UserViewHolder) {
            getItem(position).let { user ->
                holder.bind(createOnClickListener(user), createOnLongClickListener(user), user)
            }
        } else if (holder is HeaderGalleryViewHolder) {
            holder.bind(createOnClickCreateListener())
        }

    }

    private fun createOnClickListener(user: User): View.OnClickListener {
        return View.OnClickListener {
            context?.let {
                (it as MainActivity).selectUser(user)
            }
        }
    }

    private fun createOnLongClickListener(user: User): View.OnLongClickListener {
        return View.OnLongClickListener {
            context?.let {
                (it as MainActivity).opeationsWithUser(user)
            }
            return@OnLongClickListener true
        }
    }

    private fun createOnClickCreateListener(): View.OnClickListener {
        return View.OnClickListener {
            context?.let {
                (it as MainActivity).createUser(userCategory)
            }
        }
    }

    class UserViewHolder(
        private val binding: ItRecyclerItemBinding
    ) : CustomViewHolder(binding.root) {

        fun bind(
            listener: View.OnClickListener,
            longListener: View.OnLongClickListener,
            dbUser: User
        ) {
            with(binding) {
                text.setOnLongClickListener(longListener)
                clickListener = listener
                user = dbUser
                executePendingBindings()
            }
        }
    }

    class HeaderGalleryViewHolder(
        private val binding: ItRecyclerHeaderBinding
    ) : CustomViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener) {
            with(binding) {
                clickListener = listener
                executePendingBindings()
            }
        }
    }

    open class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

}

private class UsersDiffCallback : DiffUtil.ItemCallback<User>() {

    override fun areItemsTheSame(
        oldItem: User,
        newItem: User
    ): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(
        oldItem: User,
        newItem: User
    ): Boolean {
        return oldItem == newItem
    }
}