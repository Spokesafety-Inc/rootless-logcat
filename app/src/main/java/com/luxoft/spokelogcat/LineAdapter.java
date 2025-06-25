package com.luxoft.spokelogcat;

import static java.util.Locale.filter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

//import com.tananaev.logcat.LineAdapter.*
//import com.tananaev.logcat.StringUtils.containsIgnoreCase
import com.luxoft.spokelogcat.StringUtils;

public class LineAdapter extends RecyclerView.Adapter<LineAdapter.LineViewHolder> {
    private List<Line> linesAll = new ArrayList<>();
    private List<Line> linesFiltered = new ArrayList<>();
    private String level;
    private String keyword;

    public static class LineViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public LineViewHolder(View itemView) {
            super(itemView);
            this.textView = itemView.findViewById(R.id.itemText);
        }

        public TextView getTextView() {
            return this.textView;
        }
    }

    public List<Line> lines() {
        return linesFiltered;
    }
    public String level() { return level; }
    public String keyword() { return keyword; }

    @Override
    public int getItemCount() {
        return linesFiltered.size();
    }

    public void clear() {
        linesAll.clear();
        linesFiltered.clear();
        notifyDataSetChanged();
    }

    public void addItems(List<String> lines) {
        List<Line> linesAll = new LinkedList<>();
        for (String line : lines) {
            if (line != null) {
                linesAll.add(new Line(line));
            }
        }
        this.linesAll.addAll(linesAll);
        List<Line> linesFiltered = filter(linesAll);
        this.linesFiltered.addAll(linesFiltered);
        notifyItemRangeInserted(this.linesFiltered.size() - linesFiltered.size(), linesFiltered.size());
    }

    private List<Line> filter(List<Line> lines) {
        List<Line> linesFiltered = new ArrayList<>();
        boolean hasKeyword = !TextUtils.isEmpty(keyword);
        boolean hasLevel = !TextUtils.isEmpty(level);
        if (hasKeyword || hasLevel) {
            for (Line line : lines) {
                if (hasLevel && !StringUtils.containsIgnoreCase(line.level, level)) {
                    continue;
                }
                if (hasKeyword && !StringUtils.containsIgnoreCase(line.content, keyword)) {
                    continue;
                }
                linesFiltered.add(line);
            }
        } else {
            linesFiltered.addAll(lines);
        }
        return linesFiltered;
    }

    public void filter(String level, String keyword) {
        this.level = level;
        this.keyword = keyword;
        linesFiltered = filter(linesAll);
        notifyDataSetChanged();
    }

//    fun search(searchWord: String?) {
//        this.searchWord = searchWord
//        notifyDataSetChanged();
//    }

    @NonNull
    @Override
    public LineViewHolder onCreateViewHolder(ViewGroup parent, int viewType)  {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.line_list_item, parent, false);
        return new LineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LineViewHolder holder, int position) {
        Line item = linesFiltered.get(position);
        holder.itemView.setTag(item);
        //holder.itemView.setOnLongClickListener(onItemLongClickListener);
        holder.textView.setText(item.content);
        Context context = holder.textView.getContext();

        holder.itemView.setBackgroundColor(context.getResources().getColor(
                (position % 2 == 0) ? R.color.row_bg_color_even : R.color.row_bg_color_odd));
        switch (item.level) {
            case 'W' :
                holder.textView.setTextColor(context.getResources().getColor(R.color.colorWarning));
                break;
            case 'E' :
            case 'A' :
                holder.textView.setTextColor(context.getResources().getColor(R.color.colorError));
                break;
            default:
                holder.textView.setTextColor(context.getResources().getColor(R.color.colorNormal));
        }
    }

//    private OnLongClickListener onItemLongClickListener = { v ->
//        if (v.tag is Line) {
//            val line = v.tag as Line
//            val context = v.context
//            val builder = AlertDialog.Builder(context)
//            val menuItems = arrayOf(
//                    context.getString(R.string.menu_copy_text),
//                    context.getString(R.string.menu_pretty_json)
//            )
//                        builder.setItems(menuItems) { dialog, which ->
//                if (which == 0) {
//                    setClipboardText(context, line.content)
//                } else {
//                    showPrettyJsonDialog(context, line.content)
//                }
//            }
//            builder.show()
//            return@OnLongClickListener true
//        }
//        false
//    }

//    private fun setClipboardText(context: Context, text: String) {
//        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        val clip = ClipData.newPlainText("label", text)
//        clipboard.setPrimaryClip(clip)
//        Toast.makeText(context, R.string.message_done, Toast.LENGTH_SHORT).show()
//    }

//    private fun showPrettyJsonDialog(context: Context, text: String) {
//        try {
//            val index = text.indexOf("{")
//            val jsonObject = JSONObject(text.substring(index))
//            val jsonString = jsonObject.toString(2)
//            val builder = AlertDialog.Builder(context)
//            builder.setMessage(jsonString)
//            builder.setNegativeButton(R.string.menu_copy_text) { dialog, which ->
//                    setClipboardText(
//                            context,
//                            jsonString
//                    )
//            }
//            builder.setPositiveButton(R.string.warning_close, null)
//            builder.show()
//        } catch (ex: IndexOutOfBoundsException) {
//            Toast.makeText(context, R.string.message_not_json_string, Toast.LENGTH_SHORT).show()
//        } catch (ex: JSONException) {
//            Toast.makeText(context, R.string.message_not_json_string, Toast.LENGTH_SHORT).show()
//        }
//    }

}
