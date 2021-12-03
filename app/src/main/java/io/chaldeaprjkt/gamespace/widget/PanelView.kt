/*
 * Copyright (C) 2021 Chaldeaprjkt
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
package io.chaldeaprjkt.gamespace.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.widget.adapter.TileAdapter
import io.chaldeaprjkt.gamespace.widget.tiles.BaseTile

class PanelView : LinearLayout {
    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet?, dsAttr: Int) : super(ctx, attrs, dsAttr)

    private val themedInflater by lazy {
        LayoutInflater.from(context)
            .cloneInContext(ContextThemeWrapper(context, R.style.GameSpaceTheme))
    }
    private val tileContainer: RecyclerView
    private lateinit var adapter: TileAdapter


    init {
        themedInflater.inflate(R.layout.panel_view, this, true)
        isClickable = true
        isFocusable = true
        tileContainer = findViewById(R.id.panel_rv_tiles)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val rootInflater = FrameLayout(context)
        val tiles = themedInflater.inflate(R.layout.tiles, rootInflater, false)
        (tiles as ViewGroup).children.let {
            adapter = TileAdapter(it.map { t -> t as BaseTile }.toList())
            tileContainer.adapter = adapter
            tileContainer.layoutManager = GridLayoutManager(context, 2)
        }
    }

}