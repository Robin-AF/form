package com.feiyilin.form

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat

interface FlyFormItemCallback {
    fun onSetup(item: FylFormItem, viewHolder: RecyclerView.ViewHolder) {}
    fun onValueChanged(item: FylFormItem) {}
    fun onItemClicked(item: FylFormItem, viewHolder: RecyclerView.ViewHolder) {}
    fun onTitleImageClicked(item: FylFormItem) {}
    fun onStartReorder(item: FylFormItem, viewHolder: RecyclerView.ViewHolder): Boolean {
        return false
    }

    fun onMoveItem(src: Int, dest: Int): Boolean {
        return false
    }
}

open class FylFormRecyclerAdaptor(
    private var settings: MutableList<FylFormItem>,
    private var listener: FlyFormItemCallback? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var itemTouchHelper: ItemTouchHelper? = null
    private var recyclerView: RecyclerView? = null

    class ViewHolderItem(
        var type: String,
        var layoutId: Int,
        var viewHolderClass: Class<out FylFormViewHolder>
    )

    private var viewHolders: MutableList<ViewHolderItem> = mutableListOf()

    init {
        viewHolders = mutableListOf(
            ViewHolderItem("text", R.layout.form_item_text, FylFormTextViewHolder::class.java),
            ViewHolderItem(
                "section",
                R.layout.form_item_section,
                FylFormSectionViewHolder::class.java
            ),
            ViewHolderItem(
                "text_area",
                R.layout.form_item_text,
                FylFormTextAreaViewHolder::class.java
            ),
            ViewHolderItem(
                "action",
                R.layout.form_item_action,
                FylFormActionViewHolder::class.java
            ),
            ViewHolderItem(
                "text_group",
                R.layout.form_item_text,
                FylFormTextGroupViewHolder::class.java
            ),
            ViewHolderItem(
                "switch",
                R.layout.from_item_switch,
                FylFormSwitchViewHolder::class.java
            ),
            ViewHolderItem(
                "switch_native",
                R.layout.from_item_switch_native,
                FylFormSwitchNativeViewHolder::class.java
            ),
            ViewHolderItem("radio", R.layout.form_item_radio, FylFormRadioViewHolder::class.java),
            ViewHolderItem("nav", R.layout.form_item_nav, FylFormNavViewHolder::class.java),
            ViewHolderItem("label", R.layout.form_item_label, FylFormLabelViewHolder::class.java),
            ViewHolderItem("date", R.layout.form_item_date, FylFormDateViewHolder::class.java)
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        val touchHelper =
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.DOWN or ItemTouchHelper.UP, 0) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                }

                override fun isLongPressDragEnabled(): Boolean {
                    return false
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val src = viewHolder.adapterPosition
                    val des = target.adapterPosition
                    return onFormItemCallback.onMoveItem(src, des)
                }
            }

        itemTouchHelper = ItemTouchHelper(touchHelper)
        itemTouchHelper?.attachToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val item = viewHolders[viewType]
        val viewHolder = item.viewHolderClass.kotlin
        return viewHolder.constructors.first().call(inflater, item.layoutId, parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val s = settings[position]
        if (holder is FylFormViewHolder) {
            holder.bind(s, onFormItemCallback)
        }
    }

    override fun getItemCount(): Int {
        return settings.size
    }

    fun setSettings(settings: List<FylFormItem>) {
        this.settings = settings.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val m = settings[position]
        return viewHolders.indexOfFirst { it.type == m.type }
    }

    fun registerViewHolder(
        type: String,
        layoutId: Int,
        viewHolderClass: Class<out FylFormViewHolder>
    ): Boolean {
        viewHolders.add(ViewHolderItem(type, layoutId, viewHolderClass))
        return true
    }

    fun updateItem(item: FylFormItem) {
        val index = settings.indexOf(item)
        if (index >= 0 && index < settings.size) {
            notifyItemChanged(index)
        }
    }

    fun updateRadioGroup(group: String, selected: String) {
        for (i in 0 until settings.size) {
            val item = settings[i]
            if (item is FylFormItemRadio && item.group == group) {
                item.isOn = (item.tag == selected)
                updateItem(item)
            }
        }
    }

    private var onFormItemCallback = object : FlyFormItemCallback {
        override fun onValueChanged(item: FylFormItem) {
            if (item is FylFormItemRadio) {
                updateRadioGroup(item.group, item.tag)
            }
            listener?.onValueChanged(item)
        }

        override fun onItemClicked(item: FylFormItem, viewHolder: RecyclerView.ViewHolder) {
            val index = settings.indexOf(item)

            Handler().postDelayed({
                val act = recyclerView?.context as? Activity
                //Do something after 100ms
                act?.runOnUiThread {
                    recyclerView?.smoothScrollToPosition(index)
                }
            }, 100)
            listener?.onItemClicked(item, viewHolder)
        }

        override fun onSetup(item: FylFormItem, viewHolder: RecyclerView.ViewHolder) {
            listener?.onSetup(item, viewHolder)
        }

        override fun onStartReorder(
            item: FylFormItem,
            viewHolder: RecyclerView.ViewHolder
        ): Boolean {
            if (listener?.onStartReorder(item, viewHolder) == true)
                return true
            itemTouchHelper?.startDrag(viewHolder)
            return true
        }

        override fun onMoveItem(src: Int, dest: Int): Boolean {
            super.onMoveItem(src, dest)
            if (listener?.onMoveItem(src, dest) == true)
                return true
            val item = settings.removeAt(src)
            settings.add(dest, item)
            notifyItemMoved(src, dest)
            return true
        }

        override fun onTitleImageClicked(item: FylFormItem) {
            listener?.onTitleImageClicked(item)
        }
    }
}

open class FylFormViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(resource, parent, false)) {

    var titleView: TextView? = null
    var subtitleView: TextView? = null
    var titleImageView: ImageView? = null
    var reorderView: ImageView? = null

    init {
        titleView = itemView.findViewById(R.id.formElementTitle)
        subtitleView = itemView.findViewById(R.id.formElementSubTitle)
        titleImageView = itemView.findViewById(R.id.formElementTitleImage)
        reorderView = itemView.findViewById(R.id.formElementReorder)
    }

    open fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        itemView.setOnClickListener {
            listener?.onItemClicked(s, this)
        }
        titleView?.text = s.title
        if (s.title.isNotEmpty()) {
            titleView?.visibility = View.VISIBLE
        } else {
            titleView?.visibility = View.GONE
        }
        subtitleView?.text = s.subTitle
        if (s.subTitle.isNotEmpty()) {
            subtitleView?.visibility = View.VISIBLE
        } else {
            subtitleView?.visibility = View.GONE
        }

        titleImageView?.layoutParams?.height = dpToPx(s.iconSize)
        titleImageView?.layoutParams?.width = dpToPx(s.iconSize)
        titleImageView?.setImageDrawable(s.iconTitle)
        if (s.iconTitle != null) {
            titleImageView?.visibility = View.VISIBLE
            titleImageView?.setOnClickListener {
                listener?.onTitleImageClicked(s)
            }
        } else {
            titleImageView?.visibility = View.GONE
        }

        reorderView?.visibility = if (s.dragable) View.VISIBLE else View.GONE
        reorderView?.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN)
                listener?.onStartReorder(s, this)
            false
        }

        listener?.onSetup(s, this)
    }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()
    }
}

open class FylFormBaseTextViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormViewHolder(inflater, resource, parent) {
    var valueView: EditText? = null
    var listener: FlyFormItemCallback? = null
    var item: FylFormItem? = null

    init {
        valueView = itemView.findViewById(R.id.formElementValue)
        reorderView = itemView.findViewById(R.id.formElementReorder)
        valueView?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                item?.let {
                    if (it.value != s.toString()) {
                        it.value = s.toString()
                        listener?.onValueChanged(it)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        }
        )
        valueView?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                if (v is EditText) {
                    Handler().postDelayed({
                        v.setSelection(v.text.length)
                    }, 10)
                }
            }
        }
    }

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)

        this.listener = listener
        this.item = s
        if (s is FylFormItemText) {
            itemView.setOnClickListener {
                valueView?.requestFocus()
                if (!s.readOnly) {
                    val imm =
                        itemView.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                    if (imm is InputMethodManager) {
                        imm.showSoftInput(valueView, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }

            valueView?.textAlignment = s.textAlignment
            valueView?.isEnabled = !s.readOnly
            if (s.readOnly) {
                valueView?.setTextColor(Color.GRAY)
            } else {
                valueView?.setTextColor(Color.BLACK)
            }
            valueView?.setOnTouchListener(object : OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    return s.readOnly // the listener has consumed the event
                }
            })

            valueView?.setText(s.value)
            if (!s.readOnly && s.placeholder.isEmpty()) {
                valueView?.hint = "Enter ${s.title} here"
            } else {
                valueView?.hint = s.placeholder
            }
            if (s.imeOptions != 0) {
                valueView?.imeOptions = s.imeOptions
            } else {
                valueView?.imeOptions =
                    EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NAVIGATE_NEXT or EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS
            }
            if (s.inputType != 0) {
                valueView?.inputType = s.inputType
            }


            if (s.focused) {
                Handler().postDelayed({
                    s.focused = false
                    valueView?.requestFocus()
                    val imm =
                        itemView.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                    if (imm is InputMethodManager) {
                        imm.showSoftInput(valueView, InputMethodManager.SHOW_FORCED)
                    }
                }, 200)
            }
        }
    }

    private val valueWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            item?.value = s.toString()
            item?.let {
                listener?.onValueChanged(it)
            }
        }

        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
    }
}

open class FylFormTextViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormBaseTextViewHolder(inflater, resource, parent) {
}

class FylFormTextGroupViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormBaseTextViewHolder(inflater, resource, parent) {

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)

        valueView?.gravity = Gravity.START
        valueView?.textAlignment = View.TEXT_ALIGNMENT_TEXT_START

    }
}

class FylFormTextAreaViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormBaseTextViewHolder(inflater, resource, parent) {

    init {
        valueView?.gravity = Gravity.START
    }

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)
        if (s is FylFormItemTextArea) {
            valueView?.minLines = s.minLines
            valueView?.maxLines = s.maxLines
        }
    }
}

class FylFormSectionViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormViewHolder(inflater, resource, parent) {

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)

        titleView?.text = s.title.toUpperCase()
    }
}

class FylFormActionViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormViewHolder(inflater, resource, parent) {
    private var leftSpace: Space? = null
    private var rightSpace: Space? = null

    init {
        leftSpace = itemView.findViewById(R.id.formSapceLeft)
        rightSpace = itemView.findViewById(R.id.formSapceRight)
    }

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)
        if (s is FylFormItemAction) {
            when (s.alignment) {
                Gravity.CENTER -> {
                    leftSpace?.visibility = View.VISIBLE
                    rightSpace?.visibility = View.VISIBLE
                }
                Gravity.LEFT -> {
                    leftSpace?.visibility = View.GONE
                    rightSpace?.visibility = View.VISIBLE
                }
                Gravity.RIGHT -> {
                    leftSpace?.visibility = View.VISIBLE
                    rightSpace?.visibility = View.GONE
                }
            }
        }
    }
}

class FylFormSwitchNativeViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormViewHolder(inflater, resource, parent) {
    private var switchView: Switch? = null

    init {
        switchView = itemView.findViewById(R.id.formElementSwitch)
    }

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)
        if (s is FylFormItemSwitchNative) {
            itemView.setOnClickListener {
                s.isOn = !s.isOn
                switchView?.isChecked = s.isOn
                listener?.onValueChanged(s)
            }

            switchView?.setOnClickListener {
                s.isOn = !s.isOn
                listener?.onValueChanged(s)
            }
            switchView?.isChecked = s.isOn
        }
    }
}

class FylFormSwitchViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormViewHolder(inflater, resource, parent) {
    private var switchView: ImageView? = null
    private var item: FylFormItemRadio? = null

    init {
        switchView = itemView.findViewById(R.id.formElementSwitch)
    }

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)

        if (s is FylFormItemSwitch) {
            itemView.setOnClickListener {
                s.isOn = !s.isOn
                setSwitchImage(s.isOn)
                listener?.onValueChanged(s)
            }
            setSwitchImage(s.isOn)
        }
    }

    fun setSwitchImage(checked: Boolean) {
        if (checked) {
            if (item?.iconOn != null) {
                switchView?.setImageDrawable(item?.iconOn)
            } else {
                switchView?.setImageResource(R.drawable.ic_form_toggle_on)
            }
        } else {
            if (item?.iconOff != null) {
                switchView?.setImageDrawable(item?.iconOff)
            } else {
                switchView?.setImageResource(R.drawable.ic_form_toggle_off)
            }
        }
    }
}


class FylFormRadioViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormViewHolder(inflater, resource, parent) {
    private var radioView: ImageView? = null
    private var item: FylFormItemRadio? = null

    init {
        radioView = itemView.findViewById(R.id.formElementRadio)
    }

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)
        itemView.setOnClickListener {
            if (s is FylFormItemRadio) {
                if (s.isOn) {
                    return@setOnClickListener
                }
                s.isOn = !s.isOn
                //setRadioImage(s.isOn)
                listener?.onValueChanged(s)
            }
        }
        if (s is FylFormItemRadio) {
            item = s
            setRadioImage(s.isOn)
        }
    }

    fun setRadioImage(checked: Boolean) {
        if (checked) {
            if (item?.iconOn != null) {
                radioView?.setImageDrawable(item?.iconOn)
            } else {
                radioView?.setImageResource(R.drawable.ic_form_radio_on)
            }
        } else {
            if (item?.iconOff != null) {
                radioView?.setImageDrawable(item?.iconOff)
            } else {
                radioView?.setImageResource(R.drawable.ic_form_radio_off)
            }
        }
    }
}

class FylFormNavViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormViewHolder(inflater, resource, parent) {
    var badgeView: ConstraintLayout? = null
    var badgeViewTitle: TextView? = null

    init {
        badgeView = itemView.findViewById(R.id.formElementBadge)
        badgeViewTitle = itemView.findViewById(R.id.formElementBadgeTitle)
    }

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)
        if (s is FylFormItemNav) {
            if (s.badge == null) {
                badgeView?.visibility = View.GONE
            } else if (s.badge?.isEmpty() ?: true) {
                // dot
                badgeView?.visibility = View.VISIBLE
                badgeViewTitle?.visibility = View.GONE
                badgeView?.minHeight = dpToPx(10)
                badgeView?.minWidth = dpToPx(10)
            } else {
                badgeView?.visibility = View.VISIBLE
                badgeViewTitle?.visibility = View.VISIBLE
                badgeView?.minHeight = dpToPx(20)
                badgeView?.minWidth = dpToPx(20)
            }
            if (titleImageView?.visibility == View.VISIBLE) {
                titleImageView?.let {
                    badgeView?.x = it.x + it.width - dpToPx(8)
                }
            } else {
                badgeView?.x = 0F
            }
        }
        listener?.onSetup(s, this)
    }
}

class FylFormLabelViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormViewHolder(inflater, resource, parent) {
}

class FylFormDateViewHolder(inflater: LayoutInflater, resource: Int, parent: ViewGroup) :
    FylFormViewHolder(inflater, resource, parent) {

    var dateView: TextView? = null
    var datePickerView: DatePicker? = null
    var timeView: TextView? = null
    var timePickerView: TimePicker? = null

    init {
        dateView = itemView.findViewById(R.id.formElementDate)
        datePickerView = itemView.findViewById(R.id.formElementDatePicker)
        timeView = itemView.findViewById(R.id.formElementTime)
        timePickerView = itemView.findViewById(R.id.formElementTimePicker)
    }

    override fun bind(s: FylFormItem, listener: FlyFormItemCallback?) {
        super.bind(s, listener)
        if (s is FylFormItemDate) {
            dateView?.setOnClickListener {
                if (datePickerView?.visibility == View.GONE)
                    datePickerView?.visibility = View.VISIBLE
                else
                    datePickerView?.visibility = View.GONE
                timePickerView?.visibility = View.GONE
                listener?.onItemClicked(s, this)
            }
            timeView?.setOnClickListener {
                datePickerView?.visibility = View.GONE
                if (timePickerView?.visibility == View.GONE) {
                    timePickerView?.visibility = View.VISIBLE
                } else {
                    timePickerView?.visibility = View.GONE
                }
                listener?.onItemClicked(s, this)
            }

            timePickerView?.setOnTimeChangedListener { _, hour, minute ->
                s.hour = hour
                s.minute = minute
                timeView?.text = SimpleDateFormat(s.timeFormat).format(s.date)
                listener?.onValueChanged(s)
            }
            dateView?.text = SimpleDateFormat(s.dateFormat).format(s.date)
            timeView?.text = SimpleDateFormat(s.timeFormat).format(s.date)

            if (s.dateOnly) {
                timeView?.visibility = View.GONE
                timePickerView?.visibility = View.GONE
            } else {
                timeView?.visibility = View.VISIBLE
                timePickerView?.hour = s.hour
                timePickerView?.minute = s.minute
            }
            if (s.timeOnly) {
                dateView?.visibility = View.GONE
                datePickerView?.visibility = View.GONE
            } else {
                dateView?.visibility = View.VISIBLE
                datePickerView?.init( s.year, s.month, s.day) { _, year, month, dayOfMonth ->
                    s.year = year
                    s.month = month
                    s.day = dayOfMonth
                    dateView?.text = SimpleDateFormat(s.dateFormat).format(s.date)
                    listener?.onValueChanged(s)
                }
            }
        }
    }
}