/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import LibraryLB.Log;
import connectedcomponentlabeling.ConnectedComponentLabeling.MiniComponent;
import connectedcomponentlabeling.ConnectedComponentLabeling.iTableFunction;
import connectedcomponentlabeling.OptimizedAPI.MiniShared;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Lemmin
 */
public class RosenfeldPfaltz {
    public static AtomicInteger findCount = new AtomicInteger(0);
    public static int threadCount = 1;
    public static SimpleComponent[][] fromPixelArraySimple(Integer[][] pixels){
        int width = pixels.length;
        int length = pixels[0].length;
        SimpleComponent[][] array = new SimpleComponent[width][length];
        for(int i=0;i<width;i++){
            for(int j=0; j<length; j++){
                SimpleComponent comp = new SimpleComponent(i,j,pixels[i][j]);
                array[i][j] = comp; 
            }
        } 
        return array;
    }
    public static HashMap<Integer,Integer> lookUpCache = new HashMap<>();
    public static UnionFind uf = new UnionFind();
    public static AtomicInteger currentLabel = new AtomicInteger(1);
    public static ExecutorService exe;
    public static ExecutorService unionService;// = Executors.newSingleThreadExecutor();
    public static void unionRequest(int min, int max){
        if(!uf.map.containsKey(min)){
            uf.add(min);
        } 
        if(!uf.map.containsKey(max)){
            uf.add(max);  
        }
        Runnable run = new Runnable(){
            @Override
            public void run() {
                uf.union(min, max);
            }
        };
        unionService.submit(run);
    }
    public static void lookUpCache(final SimpleComponent comp){
        final int id = comp.intLabel;
        if(lookUpCache.containsKey(id)){
            comp.label = lookUpCache.get(id)+"";
        }else{
            if(!uf.map.containsKey(id)){
                comp.label = id+"";
            }else{
                int find = (int) uf.find(id);
                lookUpCache.put(id, find);
                comp.label = find+"";
                findCount.incrementAndGet();
            }
        }
        
    }
    public static class SimpleShared extends MiniShared{
        public SimpleShared(SimpleComponent[][] array){
            super(array);
        }
        @Override
        protected SimpleComponent get(int y, int x){
            if(y<this.width() && x<this.length()){
                return (SimpleComponent) this.comp[y][x];
            }else{
                return null;
            }
        }
        @Override
        protected SimpleComponent get(ConnectedComponentLabeling.Pos pos){
            if(pos == null){
                return null;
            }
            return get(pos.y,pos.x);
        }
    }
    public static class SimpleComponent extends MiniComponent{
        public int intLabel;
        public SimpleComponent(int Y, int X, int id) {
            super(Y, X, id);
            intLabel = 0;
        }
        
        
    }
    public static class RowMarker extends RowRemarker implements Callable{
        public LinkedBlockingDeque<SimpleComponent> row;
        public RowMarker topRow;

        
        public RowMarker(SimpleShared shared,int rowIndex,RowMarker dependency){
            super(shared,rowIndex);
            this.topRow = dependency;
            this.row = new LinkedBlockingDeque<>(shared.length()+1);
        }
        
        @Override
        public Object call() throws Exception {
//            Log.print("Started "+rowIndex);
            int size = shared.length();
            if(rowIndex == 0){//I AM FIRST
                SimpleComponent temp = shared.get(0, 0);
                temp.intLabel = currentLabel.get();
                row.putLast(temp);
                for(int i=1;i<size;i++){
                    SimpleComponent get = shared.get(rowIndex, i);
                    SimpleComponent left = shared.get(rowIndex, i-1);
                    if(left.id == get.id){
                        get.intLabel = left.intLabel;
                    }else{
                        get.intLabel = currentLabel.incrementAndGet();
                    }
                    row.putLast(get);
                }
            }else{
                SimpleComponent get = shared.get(rowIndex, 0);
                SimpleComponent top = topRow.row.takeFirst();
                if(get.id == top.id){
                    get.intLabel = top.intLabel;
                }else{
                    get.intLabel = currentLabel.incrementAndGet();
                }
                row.putLast(get);
                SimpleComponent left;
                for(int i=1;i<size;i++){
//                    Log.print(i);
                    get = shared.get(rowIndex, i);
                    left = shared.get(rowIndex, i-1);
                    top = topRow.row.takeFirst();
                    if(top.id != get.id && left.id != get.id){
                        get.intLabel = currentLabel.incrementAndGet();
                    }else if(top.id == get.id && left.id != get.id){
                        get.intLabel = top.intLabel;
                    }else if(top.id != get.id && left.id == get.id){
                        get.intLabel = left.intLabel;
                    }else if(top.id == get.id && left.id == get.id){
                        if(left.intLabel!= top.intLabel){ // add relation
                            int min,max;
                         
                            if(left.intLabel>top.intLabel){
                                max = left.intLabel;
                                min = top.intLabel;
                            }else{
                                min = left.intLabel;
                                max = top.intLabel;
                            }
                            get.intLabel = min;
                            unionRequest(min,max);
                            
                        }else {
                            get.intLabel = top.intLabel;
                        }
                    }else{
                        get.intLabel = currentLabel.incrementAndGet();
                    }
                    row.putLast(get);
                }
            }
//            Log.print(this.row.toString());
//            Log.print("Finished "+rowIndex);
            latch.countDown();
            return null;
        }
        
    }
    
    public static class RowRemarker implements Callable{
        public int rowIndex;
        public SimpleShared shared;
        public CountDownLatch latch;
        public RowRemarker(SimpleShared shared,int rowIndex){
            this.rowIndex = rowIndex;
            this.shared = shared;
        }
        @Override
        public Object call() throws Exception {
            int size = shared.length();
            for(int i=0; i<size; i++){
                
                SimpleComponent get = shared.get(rowIndex, i);
                lookUpCache(get);
            }
            latch.countDown();
            return null;
        }
        
    }
    
    public static void strategy(SimpleShared shared) throws InterruptedException, Exception{
        iTableFunction printIntLabel = new iTableFunction() {
            String line = "";
            int maxWidth = 3;
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                SimpleComponent name = (SimpleComponent)array[x][y];
                String app= name.intLabel+"";
                while(app.length()<maxWidth){
                    app+=" ";
                }
                line += app;               
            }

            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                Log.print(line);
                line = "";
            }
        };
        exe = Executors.newFixedThreadPool(threadCount);
        unionService = Executors.newSingleThreadExecutor();
        int width = shared.width();
        ArrayList<RowRemarker> workers = new ArrayList<>(width);
        RowMarker marker = new RowMarker(shared,0,null);
        workers.add(marker);
        for(int i=1; i<width; i++){
            RowMarker next = new RowMarker(shared,i,marker);
            marker = next;
            workers.add(next);
        }
        CountDownLatch latch = new CountDownLatch(width);

        for(RowRemarker cal:workers){
            cal.latch = latch;
            exe.submit(cal);
        }
        latch.await();
        Log.print();
        CountDownLatch latch2 = new CountDownLatch(width);
        for(int i=0; i<width; i++){
            RowRemarker remarker = new RowRemarker(shared,i);
            remarker.latch = latch2;
            exe.submit(remarker);
        }
        latch2.await();
        exe.shutdown();
        unionService.shutdown();
        unionService.awaitTermination(1, TimeUnit.DAYS);
        Log.print(findCount.get());
//        for(UFNode node:nodes.values()){
//            Log.print(node);
//        }
//                tableFunction(shared.comp,printIntLabel);

    }
}
