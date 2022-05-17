package com.bytedance.tools.codelocator.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class TestActivity : AppCompatActivity() {

    lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        mRecyclerView = findViewById(R.id.rvw_list)

        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.adapter =
            TestAdapter(listOf("Hello", "CodeLocator", "Test", "Data", "For", "ViewHolder"))
    }

}

class TestAdapter(val data: List<String>) : RecyclerView.Adapter<TestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        return TestViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
        )
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        holder.bind(data[position])
    }

}

class TestViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    fun bind(data: String) {
        view.findViewById<TextView>(R.id.tvw_item).text = data
    }

}