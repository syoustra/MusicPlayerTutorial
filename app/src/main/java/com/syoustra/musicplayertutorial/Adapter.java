package com.syoustra.musicplayertutorial;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    ArrayList<Audio> list;

    public Adapter(ArrayList<Audio> audioList) {
        this.list = list;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflate the layout, initialize the View Holder
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycler_view, parent, false);
        return new ViewHolder(itemView);

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView play_pause;
        private TextView title;
        private TextView artist;

        public ViewHolder(View itemView) {
            super(itemView);
            play_pause = (ImageView) itemView.findViewById(R.id.play_pause);
            title = (TextView) itemView.findViewById(R.id.title);
            artist = (TextView) itemView.findViewById(R.id.artist);
        }

        public void bind(int position) {
            title.setText(list.get(position).getTitle());
            artist.setText(list.get(position).getArtist());
        }

    }

}