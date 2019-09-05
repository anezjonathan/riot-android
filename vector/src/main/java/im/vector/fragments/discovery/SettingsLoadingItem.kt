/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.fragments.discovery

import android.widget.TextView
import butterknife.BindView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.R
import im.vector.ui.epoxy.BaseEpoxyHolder
import im.vector.ui.util.setTextOrHide


@EpoxyModelClass(layout = R.layout.item_loading)
abstract class SettingsLoadingItem : EpoxyModelWithHolder<SettingsLoadingItem.Holder>() {

    @EpoxyAttribute var loadingText: String? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textView.setTextOrHide(loadingText)
    }


    class Holder : BaseEpoxyHolder() {
        @BindView(R.id.loadingText)
        lateinit var textView: TextView
    }


}
