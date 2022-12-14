package com.example.a436proj

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.a436proj.databinding.ExpandableGroupChildBinding
import com.example.a436proj.databinding.ExpandableGroupParentBinding
import java.io.Serializable
import android.content.pm.PackageManager


class GroupRecyclerViewAdapter(var context: Context, var groupModelList : MutableList<ExpandableGroupModel>, var groupActivity : AppCompatActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    var settingsClickListener : OnSettingsClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            ExpandableGroupModel.PARENT -> {GroupParentViewHolder(ExpandableGroupParentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))}

            ExpandableGroupModel.CHILD -> {GroupChildViewHolder(ExpandableGroupChildBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))}

            else -> {GroupParentViewHolder(ExpandableGroupParentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))}
        }
    }

    override fun getItemCount(): Int = groupModelList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = groupModelList[position]
        when(row.type){
            ExpandableGroupModel.PARENT -> {
                (holder as GroupParentViewHolder).groupName.text = row.groupParent.groupName
                if (row.isExpanded) {
                    holder.arrow.setImageResource(R.drawable.unexpand_arrow)
                }
                else {
                    holder.arrow.setImageResource(R.drawable.expand_arrow)
                }

                holder.arrow.setOnClickListener {
                    if (row.isExpanded) {
                        row.isExpanded = false
                        //holder.layout.setBackgroundColor(Color.WHITE)
                        holder.arrow.setImageResource(R.drawable.expand_arrow)
                        collapseRow(position)
                    }
                    else{
                        row.isExpanded = true
                        //holder.layout.setBackgroundColor(Color.GRAY)
                        holder.arrow.setImageResource(R.drawable.unexpand_arrow)
                        expandRow(position)
                    }
                }

                holder.settingsButton.setOnClickListener { settingsClickListener?.onSettingsClick(row, position) }
            }


            ExpandableGroupModel.CHILD -> {
                (holder as GroupChildViewHolder).name.text = row.groupChild.name
                holder.phoneNumber.text = row.groupChild.phoneNumber
                holder.arrow.setOnClickListener {
                    if (row.isExpanded) {
                        row.isExpanded = false
                        holder.arrow.setImageResource(R.drawable.expand_arrow)
                        holder.contactExpandableContainer.visibility = View.GONE
                        holder.contactContainer.setBackgroundResource(R.drawable.group_item_border_background)
                    }
                    else {
                        row.isExpanded = true
                        holder.arrow.setImageResource(R.drawable.unexpand_arrow)
                        holder.contactExpandableContainer.visibility = View.VISIBLE
                        holder.contactContainer.setBackgroundResource(R.drawable.group_item_expanded_border_background)
                    }
                }

                holder.callButton.setOnClickListener {
                    companionPhoneNumber = row.groupChild.phoneNumber
                    requestCallPermissionLauncher.launch("android.permission.CALL_PHONE")
                }

                holder.messageButton.setOnClickListener {
                    companionPhoneNumber = row.groupChild.phoneNumber

                    var textIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$companionPhoneNumber"))
                    startActivity(context, textIntent, null)
                }
            }
        }
    }

    private val requestCallPermissionLauncher: ActivityResultLauncher<String> =
        groupActivity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                dialPhoneNumber(companionPhoneNumber);
            } else {
                Toast.makeText(
                    context,
                    "Need permission to make phone calls.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun dialPhoneNumber(phoneNumber : String) {
        val intent = Intent("android.intent.action.CALL");
        intent.data = Uri.parse("tel:$phoneNumber");

        intent.resolveActivity(groupActivity.packageManager)?.let {
            startActivity(context, intent, null);
        }
    }

    fun interface OnSettingsClickListener {
        fun onSettingsClick(item : ExpandableGroupModel, position : Int)
    }

    override fun getItemViewType(position: Int): Int = groupModelList[position].type

    private fun expandRow(position: Int){
        val row = groupModelList[position]
        var nextPosition = position
        when (row.type) {
            ExpandableGroupModel.PARENT -> {
                Log.i("recycler contact","parent contact is ${row.groupParent.contacts!!::class.java.typeName}")
                Log.i("recycler contact","parent contact name ${row.groupParent.groupName!!::class.java.typeName}")
                for(child in row.groupParent.contacts){
                    Log.d("Testing", child.name)
                    groupModelList.add(++nextPosition, ExpandableGroupModel(ExpandableGroupModel.CHILD, child))
                }
                notifyDataSetChanged()
            }

            ExpandableGroupModel.CHILD -> {
                notifyDataSetChanged()
            }
        }
    }

    private fun collapseRow(position: Int){
        if (groupModelList.size == 0) {
            return
        }

        val row = groupModelList[position]
        var nextPosition = position + 1
        when (row.type) {
            ExpandableGroupModel.PARENT -> {
                outerloop@ while (true) {
                    //  println("Next Position during Collapse $nextPosition size is ${shelfModelList.size} and parent is ${shelfModelList[nextPosition].type}")

                    if (nextPosition == groupModelList.size || groupModelList[nextPosition].type == ExpandableGroupModel.PARENT) {
                        /* println("Inside break $nextPosition and size is ${closedShelfModelList.size}")
                         closedShelfModelList[closedShelfModelList.size-1].isExpanded = false
                         println("Modified closedShelfModelList ${closedShelfModelList.size}")*/
                        break@outerloop
                    }

                    groupModelList.removeAt(nextPosition)
                }

                notifyDataSetChanged()
            }

            ExpandableGroupModel.CHILD -> {
                nextPosition = position
                while (true) {
                    if (nextPosition == groupModelList.size || groupModelList[nextPosition].type == ExpandableGroupModel.PARENT) {
                        break
                    }

                    groupModelList.removeAt(nextPosition)
                }

                notifyDataSetChanged()
            }
        }
    }

    fun updateGroupModelList(newList : MutableList<ExpandableGroupModel>, shouldCollapse : Boolean = false, previousGroupIndex : Int = 0, shouldExpand : Boolean = false, expandParentIndex : Int = 0) {
        groupModelList = newList
        if (shouldCollapse) {
            collapseRow(previousGroupIndex)
        }

        if (shouldExpand) {
            collapseRow(expandParentIndex)
            expandRow(expandParentIndex)
        }

        notifyDataSetChanged()
    }


    inner class GroupParentViewHolder(binding: ExpandableGroupParentBinding) : RecyclerView.ViewHolder(binding.root) {
        internal var layout = binding.expandableGroupContainer
        internal var groupName : TextView = binding.groupName
        internal var arrow = binding.arrow
        internal var settingsButton = binding.settings
    }

    inner class GroupChildViewHolder(binding : ExpandableGroupChildBinding) : RecyclerView.ViewHolder(binding.root) {
        internal var layout = binding.root
        internal var arrow = binding.arrow
        internal var contactExpandableContainer = binding.contactExpandableContainer
        internal var contactContainer = binding.contactContainer
        internal var callButton = binding.callButton
        internal var messageButton = binding.messageButton

        internal var name = binding.name
        internal var phoneNumber = binding.phoneNumber

    }

    companion object {
        private var companionPhoneNumber = ""
    }
}