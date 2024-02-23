package eu.project.rapid.demo.dijkstra;

import java.io.Serializable;
import java.util.ArrayList;

public class DijkstraResult implements Serializable {
    public DijkstraResult() {
    }

    public Boolean result;
    public ArrayList<Float> newPotential;
    public ArrayList<Integer> newPotentialIndex;

    public DijkstraResult(boolean result, ArrayList<Float> newPotential, ArrayList<Integer> potentialIndex) {
        this.result = result;
        this.newPotentialIndex = potentialIndex;
        this.newPotential = newPotential;
    }
}
