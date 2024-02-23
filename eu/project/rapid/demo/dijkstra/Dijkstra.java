package eu.project.rapid.demo.dijkstra;

import eu.project.rapid.ac.DFE;
import eu.project.rapid.ac.Remote;
import eu.project.rapid.ac.Remoteable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

public class Dijkstra extends Remoteable {

    private final static Logger log = LogManager.getLogger(Dijkstra.class.getSimpleName());
    private transient DFE dfe;
    private int nrClones;

    private static final int PRIORITYBUFSIZE = 10000;
    private static final float POT_HIGH = 1.0e10F;
    private static final float INVSQRT2 = 0.707106781f;
    private int ns_ = 66560;
    private int nx_ = 416;
    private int ny_ = 160;
    private int lethal_cost_ = 253;
    private int threshold_ = lethal_cost_;
    private float priorityIncrement = 100.0F;
    private float neutral_cost_ = 50.0F;
    private float priorityIncrement_ = 2 * neutral_cost_;
    private boolean precise_ = true;
    private boolean[] pending_ = new boolean[ns_];

    private float[] potential = new float[ns_];
    private ArrayList<Float> newPotential = new ArrayList<Float>();
    private ArrayList<Integer> newPotentialIndex = new ArrayList<Integer>();

    private int currentEnd_, nextEnd_, overEnd_;

    int[] currentBuffer_ = new int[PRIORITYBUFSIZE];
    int[] nextBuffer_ = new int[PRIORITYBUFSIZE];
    int[] overBuffer_ = new int[PRIORITYBUFSIZE];

    private float factor_ = 3.0F;
    private boolean unknown_ = true;

    private int cells_visited_;


    /**
     * @param dfe      The execution dfe taking care of the execution
     * @param nrClones In case of remote execution specify the number of clones needed
     */
    public Dijkstra(DFE dfe, int nrClones) {
        this.dfe = dfe;
        this.nrClones = nrClones;
    }

    /**
     * @param dfe The execution dfe taking care of the execution
     */
    public Dijkstra(DFE dfe) {
        this(dfe, 1);
    }

    @Override
    public void prepareDataOnClient() {
    }

    public void setNumberOfClones(int nrClones) {
        this.nrClones = nrClones;
    }

    @Override
    public void copyState(Remoteable state) {

    }

    /**
     * Solve the Dijkstra problem
     * //TODO write the params
     * //@param N The number of queens
     * @return boolean
     */
    public DijkstraResult solveDijkstra(byte[] costs, double start_x, double start_y, double end_x, double end_y, int cycles, float[] potential) {
        Method toExecute;
        Class<?>[] paramTypes = {byte[].class, double.class, double.class, double.class, double.class, int.class, float[].class};
        Object[] paramValues = {costs, start_x, start_y, end_x, end_y, cycles, potential};

        DijkstraResult result = new DijkstraResult();

        try {
            toExecute = this.getClass().getDeclaredMethod("localSolveDijkstra", paramTypes);

            result = (DijkstraResult) dfe.execute(toExecute, paramValues, this);
        } catch (SecurityException e) {
            // Should never get here
            e.printStackTrace();
            throw e;
        } catch (NoSuchMethodException e) {
            // Should never get here
            e.printStackTrace();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return result;
    }

    @SuppressWarnings("unused")
    @Remote
    private DijkstraResult localSolveDijkstra(byte[] costs, double start_x, double start_y, double end_x, double end_y, int cycles, float[] potential) {
        System.out.println("------------- Solving Dijkstra...");

        DijkstraResult res;

        log.info("Finding solutions for Dijkstra puzzle.");
        log.info(String.format("Analyzing columns: \nstart_x = %f, start_y = %f,\nend_x = %f, end_y = %f\ncycles = %d",
                start_x, start_y, end_x, end_y, cycles));

        res = this.calculatePotentials(costs, start_x, start_y, end_x, end_y, cycles, potential);
        return res;
    }


    public DijkstraResult calculatePotentials(byte[] costs, double start_x, double start_y, double end_x, double end_y, int cycles, float[] potential1){
        cells_visited_ = 0;
        threshold_ = lethal_cost_;
        currentEnd_ = 0;
        nextEnd_ = 0;
        overEnd_ = 0;

        Arrays.fill(pending_, false);
        Arrays.fill(potential, POT_HIGH);
        int k = toIndex(new BigDecimal(start_x).intValue(), new BigDecimal(start_y).intValue());

        int potentialIndex = 0;

        if (precise_) {
            double dx = start_x - Math.floor(start_x);
            double dy = start_y - Math.floor(start_y);

            dx = Math.floor(dx * 100 + 0.5) / 100;
            dy = Math.floor(dy * 100 + 0.5) / 100;

            float kP = (float)(neutral_cost_ * 2 * dx * dy);
            potential[k] = kP;
            newPotential.add(kP);
            newPotentialIndex.add(k);

            potential[k+1] = (float)(neutral_cost_ * 2 * (1-dx) * dy);
            newPotential.add((float)(neutral_cost_ * 2 * (1-dx) * dy));
            newPotentialIndex.add(k+1);

            potential[k+nx_] = (float)(neutral_cost_ * 2 * dx * (1-dy));
            newPotential.add((float)(neutral_cost_ * 2 * dx * (1-dy)));
            newPotentialIndex.add(k+nx_);

            potential[k+nx_+1] = (float)(neutral_cost_ * 2 * (1-dx) * (1-dy));
            newPotential.add((float)(neutral_cost_ * 2 * (1-dx) * (1-dy)));
            newPotentialIndex.add(k+nx_+1);


            push_cur(costs, k+2);
            push_cur(costs, k-1);
            push_cur(costs, k+nx_-1);
            push_cur(costs, k+nx_+2);
            push_cur(costs, k-nx_);
            push_cur(costs, k-nx_+1);
            push_cur(costs, k+nx_*2);
            push_cur(costs, k+nx_*2+1);
        } else {

            potential[k] = 0;
            newPotential.add(0f);
            newPotentialIndex.add(k);

            push_cur(costs, k+1);
            push_cur(costs, k-1);
            push_cur(costs, k-nx_);
            push_cur(costs, k+nx_);
        }
        int nwv = 0;            // max priority block size
        int nc = 0;            // number of cells put into priority blocks
        int cycle = 0;        // which cycle we're on

        // set up start cell
        int startCell = toIndex(new BigDecimal(end_x).intValue(), new BigDecimal(end_y).intValue());
        Boolean result = false;

        for (; cycle < cycles; cycle++) { // go for this many cycles, unless interrupted
            if (currentEnd_ == 0 && nextEnd_ == 0) {// priority blocks empty
                return new DijkstraResult(result, newPotential, newPotentialIndex);
            }
            // stats
            nc += currentEnd_;
            if (currentEnd_ > nwv)
                nwv = currentEnd_;

            // reset pending_ flags on current priority buffer
            for (int i = 0; i < currentEnd_; i++) {
                pending_[currentBuffer_[i]] = false;
            }

//            for (int i = 0; i < currentEnd_; i++) {
//                log.info(currentBuffer_[i]);
//            }

            // process current priority buffer
            for (int i = 0; i < currentEnd_; i++) {
                updateCell(costs, potential, currentBuffer_[i]);
            }
//            for (int i = 0; i < currentEnd_; i++) {
//                log.info(currentBuffer_[i]);
//            }

            // swap priority blocks currentBuffer_ <=> nextBuffer_
            currentEnd_ = nextEnd_;
            nextEnd_ = 0;
            int[] temp = currentBuffer_;
            currentBuffer_ = nextBuffer_;
            nextBuffer_ = temp;

            // see if we're done with this priority level
            if (currentEnd_ == 0) {
                threshold_ += priorityIncrement_; // Increment priority threshold
                currentEnd_ = overEnd_; // Move overflow to current
                overEnd_ = 0;
                temp = currentBuffer_; // Swap buffers again for overflow
                currentBuffer_ = overBuffer_;
                overBuffer_ = temp;
            }


            // check if we've hit the Start cell
            if (potential[startCell] < POT_HIGH)
                break;
        }

        System.out.println();



        //ROS_INFO("CYCLES %d/%d ", cycle, cycles);
        if (cycle < cycles)
            result = true;
        else
            result = false;

        return new DijkstraResult(result, newPotential, newPotentialIndex);
    }

    private int toIndex(int x, int y){
        return x + nx_ * y;
    }


    public void updateCell(byte[] costs, float[] potential1, int n) {
        cells_visited_++;
        double c = getCost(costs, n);
        if (c >= lethal_cost_) return; // don't propagate into obstacles

        float prevPotential = -1; // Default value indicating to calculate min of neighbors

        float pot = calculatePotential(c, n, prevPotential);

        // FIXME ubrat n - nx_ stuku (summa)
//        if(n - nx_ < 100) n += nx_;

        // now add affected neighbors to priority blocks
        if (pot < potential[n]) { // low-cost buffer block
            double le = INVSQRT2 * getCost(costs,n - 1);
            double re = INVSQRT2 * getCost(costs,n + 1);
            double ue = INVSQRT2 * getCost(costs,n - nx_);
            double de = INVSQRT2 * getCost(costs,n + nx_);
            potential[n] = pot;
            newPotential.add(pot);
            newPotentialIndex.add(n);
            try{
                if (pot < threshold_) { // Low-cost buffer block
                    if (potential[n - 1] > pot + le) pushNext(costs,n - 1);
                    if (potential[n + 1] > pot + re) pushNext(costs,n + 1);
                    if (potential[n - nx_] > pot + ue) pushNext(costs,n - nx_);
                    if (potential[n + nx_] > pot + de) pushNext(costs,n + nx_);
                } else { // Overflow block
                    if (potential[n - 1] > pot + le) pushOver(costs,n - 1);
                    if (potential[n + 1] > pot + re) pushOver(costs,n + 1);
                    if (potential[n - nx_] > pot + ue) pushOver(costs,n - nx_);
                    if (potential[n + nx_] > pot + de) pushOver(costs,n + nx_);
                }
            } catch (Exception e){
                log.error(e.getMessage());
            }
        }
    }


    //FIXME: remove %4 from costs[n] in getCost
    private double getCost(byte[] costs, int n) {
        double c = costs[n] & 0xFF; // Convert byte to unsigned
        if (c < lethal_cost_ - 1 || (unknown_ && c == 255)) {
            c = c * factor_ + neutral_cost_;
            if (c >= lethal_cost_)
                c = lethal_cost_ - 1;
            return c;
        }
        return lethal_cost_;
    }

    // Adapted calculatePotential method in Java
    public float calculatePotential(double cost, int n, float prevPotential) {
        try {
            if (prevPotential < 0) {

                // Get min of neighbors
                //FIXME: REMOVE ? :
                double minH = n - 1 >= 0 ? Math.min(potential[n - 1], potential[n + 1]) : potential[n + 1];


                double minV = n - nx_ >= 0 ? Math.min(potential[n - nx_], potential[n + nx_]) : potential[n + nx_];
                prevPotential = (float) Math.min(minH, minV);
            }
        } catch (Exception e){
            log.error(e.getMessage());
        }


        return prevPotential + (float)cost;
    }



    // Implementing push methods
    private void pushNext(byte[] costs, int n) {
        if (n>=0 && n<ns_ && !pending_[n]
                && getCost(costs, n)<lethal_cost_
                &&    nextEnd_<PRIORITYBUFSIZE){
            nextBuffer_[   nextEnd_++]=n;
            pending_[n]=true;
        }
    }

    private void pushOver(byte[] costs, int n) {
        if (n>=0 && n<ns_ && !pending_[n]
                && getCost(costs, n)<lethal_cost_
                &&    overEnd_<PRIORITYBUFSIZE){
            overBuffer_[   overEnd_++]=n;
            pending_[n]=true;
        }
    }

    private void push_cur(byte[] costs, int n) {
        if (n>=0 && n<ns_ && !pending_[n]
                && getCost(costs, n)<lethal_cost_
                && currentEnd_<PRIORITYBUFSIZE){
            currentBuffer_[currentEnd_++]=n;
            pending_[n]=true;
        }
    }
}

