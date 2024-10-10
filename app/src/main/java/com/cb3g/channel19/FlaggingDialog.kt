package com.cb3g.channel19

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.android.multidex.myapplication.R
import com.example.android.multidex.myapplication.databinding.FlaggingDialogBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Date

class FlaggingDialog : DialogFragment() {
    lateinit var binding: FlaggingDialogBinding
    lateinit var adapter: CustomRecycler
    private lateinit var preferences: SharedPreferences
    var flagsIn: List<FlagObject> = ArrayList()
    var flagsOut: List<FlagObject> = ArrayList()
    var flagCount: Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        preferences = context.getSharedPreferences("flaggingDialog", Context.MODE_PRIVATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FlaggingDialogBinding.inflate(inflater)
        adapter = CustomRecycler(this.layoutInflater)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged") override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.close.setOnClickListener {
            Utils.vibrate(it)
            dismiss()
        }
        binding.flagsRecycler.setLayoutManager(LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false))
        binding.flagsRecycler.setHasFixedSize(true)
        binding.flagsRecycler.setAdapter(adapter)
        binding.flagsChip.isChecked = preferences.getBoolean("option", false)
        binding.flagsChip.setOnCheckedChangeListener { button, checked ->
            Utils.vibrate(button)
            if (checked) binding.flagsChip.text = getString(R.string.flags_in)
            else binding.flagsChip.text = getString(R.string.flags_out)
            updateList()
            preferences.edit().putBoolean("option", checked).apply()
        }
        adapter.notifyDataSetChanged()
        checkFlagOut()
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n") private fun updateList() {
        this.activity?.runOnUiThread {
            if (binding.flagsChip.isChecked) adapter.updateList(flagsIn)
            else adapter.updateList(flagsOut)
            adapter.notifyDataSetChanged()
            binding.boxTitle.text = "Total Flags $flagCount"
        }
    }

    private fun checkFlagOut() {
        val data = Jwts.builder().setHeader(RadioService.header).claim("userId", RadioService.operator.user_id).setIssuedAt(Date(System.currentTimeMillis())).setExpiration(Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, RadioService.operator.key).compact()
        Utils.call(RadioService.client, data, "user_flag_counting.php", object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                e.message.toString().log()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        it.body?.let { body ->
                            try {
                                val jsonObject = JSONObject(body.string())
                                flagCount = jsonObject.getInt("flags")
                                flagsIn = Gson().fromJson(jsonObject.getJSONArray("in").toString(), object : TypeToken<List<FlagObject>>() {}.type)
                                flagsOut = Gson().fromJson(jsonObject.getJSONArray("out").toString(), object : TypeToken<List<FlagObject>>() {}.type)
                                updateList()
                            } catch (e: JSONException) {
                                e.message?.log()
                            }
                        }
                    }
                }
            }
        })
    }

}

class CustomRecycler(private val inflation: LayoutInflater) : RecyclerView.Adapter<CustomRecycler.FlagViewHolder>() {
    var list: List<FlagObject> = ArrayList()

    fun updateList(update: List<FlagObject>) {
        list = update
    }

    inner class FlagViewHolder(view: View) : ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.flag_handle)
        var image: ImageView = view.findViewById(R.id.flag_drawable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlagViewHolder {
        return FlagViewHolder(inflation.inflate(R.layout.flag_row, parent, false))
    }

    override fun onBindViewHolder(holder: FlagViewHolder, position: Int) {
        "OnBind $position".log()
        val entry = list[position]
        holder.title.text = entry.handle
        if (entry.count == 5) holder.image.setImageResource(R.drawable.flag_red)
        else holder.image.setImageResource(R.drawable.flag)
    }

    override fun getItemCount(): Int {
        Gson().toJson(list).log()
        "size ${list.size}".log()
        return list.size
    }
}

class FlagObject(val handle: String, val count: Int)