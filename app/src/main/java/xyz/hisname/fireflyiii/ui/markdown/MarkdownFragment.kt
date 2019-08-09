package xyz.hisname.fireflyiii.ui.markdown

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.fragment_markdown.*
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.MarkdownViewModel
import xyz.hisname.fireflyiii.ui.base.BaseFragment
import xyz.hisname.fireflyiii.util.extension.*
import xyz.hisname.fireflyiii.util.extension.getViewModel

class MarkdownFragment: BaseFragment() {

    private val toolbar by lazy {requireActivity().findViewById<Toolbar>(R.id.activity_toolbar) }
    private val markdownViewModel by lazy { getViewModel(MarkdownViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.create(R.layout.fragment_markdown, container)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setWidget()
        parseText()
        handleClick()
    }

    private fun setWidget(){
        toolbar.visibility = View.GONE
        discardButton.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_close)
                        .color(getCompatColor(R.color.md_white_1000))
                        .sizeDp(12),null, null, null)
        doneButton.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_done)
                        .color(getCompatColor(R.color.md_white_1000))
                        .sizeDp(12),null, null, null)
        boldMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_format_bold)
                        .color(getCompatColor(R.color.md_black_1000))
                        .sizeDp(18))
        italicMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_format_italic)
                        .color(getCompatColor(R.color.md_black_1000))
                        .sizeDp(18))
        hyperlinkMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_insert_link)
                        .color(getCompatColor(R.color.md_black_1000))
                        .sizeDp(18))
        strikeThroughMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_format_strikethrough)
                        .color(getCompatColor(R.color.md_black_1000))
                        .sizeDp(18))
        quoteMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_format_quote)
                        .color(getCompatColor(R.color.md_black_1000))
                        .sizeDp(18))
        bulletMarkdown.setImageDrawable(
                IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_format_list_bulleted)
                        .color(getCompatColor(R.color.md_black_1000))
                        .sizeDp(18))
        editableText.setText(markdownViewModel.markdownText.value)
        discardButton.setOnClickListener {
            handleBack()
        }
        doneButton.setOnClickListener {
            markdownViewModel.markdownText.postValue(editableText.getString())
            handleBack()
        }
    }

    private fun parseText(){
        val extensions = arrayListOf(StrikethroughExtension.create(), AutolinkExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        editableText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(editable: Editable) {

            }

            override fun beforeTextChanged(charsequence: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun onTextChanged(charsequence: CharSequence, start: Int, before: Int, count: Int) {
                val document = parser.parse(charsequence.toString())
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    displayText.text = Html.fromHtml(renderer.render(document), Html.FROM_HTML_MODE_COMPACT)
                } else {
                    displayText.text = Html.fromHtml(renderer.render(document))
                }
            }

        })

    }

    private fun handleClick(){
        boldMarkdown.setOnClickListener {
            editableText.append("****")
        }
        italicMarkdown.setOnClickListener {
            editableText.append("**")
        }
        strikeThroughMarkdown.setOnClickListener {
            editableText.append("~~~~")
        }
        quoteMarkdown.setOnClickListener {
            editableText.append(">")
        }
        bulletMarkdown.setOnClickListener {
            editableText.append("•")
        }
        hyperlinkMarkdown.setOnClickListener {
            val layoutView = layoutInflater.inflate(R.layout.dialog_hyperlink, null)
            val urlText = layoutView.findViewById<EditText>(R.id.linktextEditText)
            urlText.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(requireContext())
                    .icon(GoogleMaterial.Icon.gmd_format_underlined)
                    .sizeDp(16),null, null, null)
            val url = layoutView.findViewById<EditText>(R.id.linkEditText)
            url.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(requireContext())
                    .icon(GoogleMaterial.Icon.gmd_insert_link)
                    .sizeDp(16),null, null, null)
            val alert = AlertDialog.Builder(requireContext())
            alert.apply {
                setTitle("Insert Link")
                setView(layoutView)
                setPositiveButton("OK") { dialogInterface, which ->
                    editableText.append("[" + urlText.getString() + "]" + "(" + url.getString() + ")")
                }
                setNegativeButton("Cancel"){ dialogInterface, which ->

                }
                show()
            }

        }
    }

    override fun handleBack() {
        requireFragmentManager().popBackStack()
        toolbar.visibility = View.VISIBLE
        hideKeyboard()
    }
}