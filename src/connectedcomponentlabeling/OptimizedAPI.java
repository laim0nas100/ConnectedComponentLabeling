/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import connectedcomponentlabeling.ConnectedComponentLabeling.MiniComponent;
import connectedcomponentlabeling.ConnectedComponentLabeling.Pos;
import static connectedcomponentlabeling.ConnectedComponentLabeling.getUnusedLabel;
import static connectedcomponentlabeling.ConnectedComponentLabeling.join;
import static connectedcomponentlabeling.ConnectedComponentLabeling.printLabel;
import static connectedcomponentlabeling.ConnectedComponentLabeling.sortByComponent;
import static connectedcomponentlabeling.ConnectedComponentLabeling.sortByPos;
import static connectedcomponentlabeling.ConnectedComponentLabeling.start;
import static connectedcomponentlabeling.ConnectedComponentLabeling.tableFunction;
import static connectedcomponentlabeling.ConnectedComponentLabeling.transpose;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;


/**
 *
 * @author Lemmin
 */
public class OptimizedAPI {
    
    public static MiniComponent[][] fromPixelArrayMini(Integer[][] pixels){
        int width = pixels.length;
        int length = pixels[0].length;
        MiniComponent[][] array = new MiniComponent[width][length];
        for(int i=0;i<width;i++){
            for(int j=0; j<length; j++){
                MiniComponent comp = new MiniComponent(i,j,pixels[i][j]);
                array[i][j] = comp; 
            }
        } 
        return array;
    }
   
    public static class CompSet{
        public HashSet<Pos> topPos;
        public HashSet<MiniComponent> bottom;
        public HashSet<MiniComponent> collected;
        public HashSet<MiniComponent> recentlyAdded;
        public boolean added = false;
        public HashSet<Pos> getConnectedBottomPos(){
            HashSet<Pos> pos = new HashSet<>();
            bottom.forEach(component ->{
                if(component.down != null){
                    pos.add(component.down);
                }
            });
            return pos;
        }
        public CompSet(){
            this.topPos = new HashSet<>();
            this.bottom = new HashSet<>();
            this.collected = new HashSet<>();
            this.recentlyAdded = new HashSet<>();
        }
        @Override
        public String toString(){
            String res = "" ;
            res += "top:\n" + sortByPos(topPos)+"\n";
            res += "collected:\n"+ sortByComponent(collected)+"\n";
            res += "bottom:\n"+sortByPos(getConnectedBottomPos());
            return res;
        }
    }
    public static class MiniShared{
        public MiniComponent[][] comp;
        public final int length,width;
        public MiniShared(MiniComponent[][] array){
            this.comp = array;
            this.width = array.length;
            this.length = array[0].length;
        }
        protected MiniComponent get(int y, int x){
            if(y<this.width && x<this.length){
                return this.comp[y][x];
            }else{
                return null;
            }
        }
        protected MiniComponent get(Pos pos){
            if(pos == null){
                return null;
            }
            return get(pos.y,pos.x);
        }
    }
    
    public static abstract class DependableWorker extends MiniShared implements Callable{
        public CountDownLatch latch;
        public ArrayList<CountDownLatch> dependencies;
        public DependableWorker(MiniComponent[][] array) {
            super(array);
            this.dependencies = new ArrayList<>();
            this.latch = new CountDownLatch(1);
        }
        
        public void waitForDependencies() throws InterruptedException{
            for(CountDownLatch l:dependencies){
                l.await();
            }
        }
        public abstract void logic();
        @Override
        public final Object call() throws Exception {
            waitForDependencies();
            logic();
            latch.countDown();
            return null;
        }
        
    
}
    public static class UltimateWorker extends DependableWorker{
        private final int workLine;
        public ArrayDeque<HashSet<MiniComponent>> iterated;
        public ArrayList<CompSet> compSet;
        public UltimateWorker(MiniComponent[][] array, int workLine) {
            super(array);
            this.workLine = workLine;
            this.compSet = new ArrayList<>();
            this.iterated = new ArrayDeque<>();
            
        }

        @Override
        public void logic() {
            int index = 0;
            this.iterated.add(new HashSet<>());
            MiniComponent prev;
            MiniComponent next = this.comp[workLine][index];
            MiniComponent down = this.get(workLine+1, index);
            if(down!=null && down.id == next.id){
                next.down = down.location;
            }
            index++;
            while(index<this.length){
                this.iterated.getLast().add(next);
                prev = next;
                next = this.get(workLine, index);
                if(prev.id != next.id){
                    this.iterated.add(new HashSet<>());
                }
                down = this.get(workLine+1, index);
                if(down!=null && down.id == next.id){
                    next.down = down.location;
                }
                index++;
            }
            for(HashSet<MiniComponent> set:iterated){
                CompSet cset = new CompSet();
                cset.bottom.addAll(set);
                set.forEach(component ->{
                    cset.topPos.add(component.location);
                });
                cset.collected.addAll(set);
                compSet.add(cset);
            }
        }
        
    } 
    public static class WorkerMerger extends DependableWorker{
        
//        public WorkerMerger depTop,depBot;
        public UltimateWorker top,bot;

        public WorkerMerger(MiniComponent[][] array,UltimateWorker topWork,UltimateWorker botWork) {
            super(array);
            top = topWork;
            bot = botWork;
        }
        
        public <T> boolean hasSameElement(Collection<T> col1, Collection<T> col2){
            if(col1.isEmpty()||col2.isEmpty()){
                return false;
            }
            if(col1.size()<col2.size()){
                for(T el:col1){
                    if(col2.contains(el)){
                        return true;
                    }
                }
            }else{
                for(T el:col2){
                    if(col1.contains(el)){
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        
        @Override
        public void logic(){
            ArrayList<CompSet> topSet = top.compSet;  
            ArrayList<CompSet> botSet = bot.compSet;
            int i =-1;
            int topLimit = topSet.size()-1;
            int botLimit = botSet.size()-1;
            boolean endOuter = false;
            boolean endInner = false;
            while(i<topLimit && !endOuter){
                i++;
                CompSet set = topSet.get(i);
                HashSet<Pos> connectedBottomPos = set.getConnectedBottomPos();
                
                ArrayList<CompSet> matchedSets = new ArrayList<>();
                int j = -1;
                while(j<botLimit && !endInner){
                    j++;
                    CompSet otherSet = botSet.get(j);
                    if(hasSameElement(connectedBottomPos,otherSet.topPos)){
                        matchedSets.add(otherSet);
                        otherSet.added = true;
                    }
                }
                set.bottom.clear();
                if(!matchedSets.isEmpty()){
                    set.recentlyAdded.clear();
                    for(CompSet matched:matchedSets){
                        set.bottom.addAll(matched.bottom);
                        set.collected.addAll(matched.collected);
                    }
                }   
            }
            //find new set
            for(CompSet bots:botSet){              
                if(!bots.added){
                    topSet.add(bots);
                }
            }
            //merge sets
            ArrayList<CompSet> newSets = new ArrayList<>();
            while(!topSet.isEmpty()){
                CompSet set = topSet.remove(0);
                Iterator<CompSet> iterator = topSet.iterator();
                while(iterator.hasNext()){
                    CompSet other = iterator.next();
                    if(hasSameElement(set.collected,other.collected)){
                        set.topPos.addAll(other.topPos); 
                        set.bottom.addAll(other.bottom);
                        set.collected.addAll(other.collected);
                        iterator.remove();
                    }
                }
                newSets.add(set);
                
            }
            topSet.clear();
            topSet.addAll(newSets);
        }

        
        public String id(){
            return top.workLine+" "+bot.workLine;
        }
        @Override
        public String toString(){
            String res = id();
            
            return res;
        }
        
    }
    
    public static void optimizedStrategy(Integer[][] pixels) throws InterruptedException{
        MiniComponent[][] fromPixels = fromPixelArrayMini(pixels);
        
        boolean didTranspose = false;
        long transposeOverhead = System.currentTimeMillis();
        
        if(fromPixels.length > fromPixels[0].length){
            fromPixels = transpose(fromPixels);
            didTranspose = true;
        }
        transposeOverhead = System.currentTimeMillis() - transposeOverhead;
        
        MiniShared shared = new MiniShared(fromPixels);
        UltimateWorker firstWorker;
        ArrayList<UltimateWorker> workers = new ArrayList<>();
        ArrayList<WorkerMerger> mergers = new ArrayList<>();
        HashMap<Integer,DependableWorker> dependables = new HashMap<>();
        for(int i = 0; i<shared.width; i++){
            UltimateWorker worker = new UltimateWorker(shared.comp,i);
            workers.add(worker);
            dependables.put(i, worker);
        }
        firstWorker = workers.get(0);
        int width = shared.width;
        int increment = 2;
        
        do{
            int offset = increment/2;
            int count = 0;
            for(int i=0; i+offset<width; i+= increment){
                int top = i;
                int bot = i + offset;
                System.out.println(top+" "+(bot));
                WorkerMerger merger = new WorkerMerger(shared.comp,workers.get(top),workers.get(bot));
                merger.dependencies.add(dependables.remove(bot).latch);
                merger.dependencies.add(dependables.remove(top).latch);
                dependables.put(top, merger);
                mergers.add(merger);
                count++;
            }
            increment*=2;
            System.out.println("#### "+increment +" parallelization: "+count);
        }while(increment/2<width);
        
        ArrayList<DependableWorker> all = new ArrayList<>();
        all.addAll(workers);
        all.addAll(mergers);
        long time = System.currentTimeMillis();
        start(all);
        join(all);
        time = System.currentTimeMillis() - time;
        
        for(CompSet comp:firstWorker.compSet){
            String label = getUnusedLabel();
            for(MiniComponent component:comp.collected){
                component.label = label;
            }
        }
        long transposeOverhead2 = System.currentTimeMillis();
        if(didTranspose){
            shared.comp = transpose(shared.comp);
        }
        transposeOverhead2 = System.currentTimeMillis() - transposeOverhead2;
        tableFunction(shared.comp,printLabel);
        System.out.println("\n"+time);
        System.out.println("transpose overhead "+ transposeOverhead +" "+transposeOverhead2);
        System.out.println("Total:"+(time+ transposeOverhead + transposeOverhead2));
        
    }
}

/*

Blank test
512 X 4096
11107
12012
11180
12277
12218
11748

4096 X 512
11026
10281
11366
10512
10491


Random test
512 X 4096
23991
22665
20961

4096 X 512
14374
14340
14568







*/
