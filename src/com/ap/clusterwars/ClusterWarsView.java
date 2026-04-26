package com.ap.clusterwars;

import static com.raylib.Raylib.DrawLine;

public class ClusterWarsView {

    private final ClusterWarServer gameServer;

    public ClusterWarsView(ClusterWarServer gameServer) {
        this.gameServer = gameServer;
    }

//    private  void drawGrid() {
//        for (int i = 0; i < GRID_SIZE; i++) {
//            DrawLine(i * CELL_SIZE, 0, i * CELL_SIZE, 800, DARKGRAY);
//            DrawLine(0, i * CELL_SIZE, 800, i * CELL_SIZE, DARKGRAY);
//        }
//    }
}
