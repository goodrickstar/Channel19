package com.cb3g.channel19

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
import androidx.recyclerview.widget.RecyclerView.inflate
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
    var flagsIn: List<FlagObect> = ArrayList()
    var flagsOut: List<FlagObect> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FlaggingDialogBinding.inflate(inflater)
        adapter = CustomRecycler(this.layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.close.setOnClickListener {
            Utils.vibrate(it)
            dismiss()
        }
        binding.flagsRecycler.setLayoutManager(LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false))
        binding.flagsRecycler.setHasFixedSize(true)
        binding.flagsRecycler.setAdapter(adapter)
        binding.flagsChip.setOnCheckedChangeListener { button, checked ->
            Utils.vibrate(button)
            if (checked) binding.flagsChip.text = "Flags In"
            else binding.flagsChip.text = "Flags Out"
            updateList()
        }
        checkFlagOut()
    }

    private fun updateList() {
        this.activity?.runOnUiThread {
            if (binding.flagsChip.isChecked) {
                adapter.updateList(flagsIn)
            } else {
                adapter.updateList(flagsOut)
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun checkFlagOut() {
        val data = Jwts.builder().setHeader(RadioService.header).claim("userId", RadioService.operator.user_id).setIssuedAt(Date(System.currentTimeMillis())).setExpiration(Date(System.currentTimeMillis() + 60000)).signWith(SignatureAlgorithm.HS256, RadioService.operator.key).compact()
        UtilsK().call("http://23.111.159.2/~channel1/" + "public/data.php", RadioService.operator.user_id, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { body -> //body.string().log()
                        try {
                            val jsonObject = JSONObject(body.string())
                            val flagsInData = jsonObject.getJSONArray("in").toString()
                            val flagsOutData = jsonObject.getJSONArray("out").toString()
                            flagsIn = Gson().fromJson(flagsInData, object : TypeToken<List<FlagObect>>() {}.type)
                            flagsOut = Gson().fromJson(flagsOutData, object : TypeToken<List<FlagObect>>() {}.type)


                        } catch (e: JSONException) {
                            e.message?.log()
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.let { it.message?.log() }
            }
        })
    }


}

class CustomRecycler(private val inflator: LayoutInflater) : RecyclerView.Adapter<CustomRecycler.FlagViewHolder>() {
    var list: ArrayList<FlagObect> = ArrayList()

    fun updateList(update: List<FlagObect>) {
        list.clear()
        list.addAll(update)
    }

    inner class FlagViewHolder(view: View) : ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.flag_handle)
        var image: ImageView = view.findViewById(R.id.flag_drawable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlagViewHolder {
        return FlagViewHolder(inflator.inflate(R.layout.flag_row, parent, false))
    }

    override fun onBindViewHolder(holder: FlagViewHolder, position: Int) {
        val entry = list[position]
        holder.title.text = entry.handle
        if (entry.count == 5) holder.image.setImageResource(R.drawable.flag_red)
    }

    override fun getItemCount(): Int {
        "List size ${list.size}".log()
        return list.size
    }



}

class FlagObect(val handle: String, val count: Int)