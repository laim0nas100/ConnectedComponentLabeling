/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import static connectedcomponentlabeling.OptimizedAPI.optimizedStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.imageio.ImageIO;

/**
 *
 * @author Lemmin
 */
public class ConnectedComponentLabeling {
    public static final int THREAD_COUNT = 10;
    public static boolean useThreads = false;
    public static ExecutorService pool;
    public static HashSet<String> set = new HashSet<>();
    public static Shared shared;
    public static int length,width;
    public static int charInt = 33;
    public static AtomicInteger relabelCount = new AtomicInteger(0);
//    public static volatile ArrayList<Store> connected = new ArrayList<>();
    
    public static String getUnusedLabel(){
        charInt++;
        String valueOf = String.valueOf(Character.toChars(charInt));
        return valueOf;
    }
    public static Integer[][] parsePicture(String path) throws IOException{
        BufferedImage image = ImageIO.read(new File(path));
        int height = image.getHeight();
        int width = image.getWidth();
        Integer[][] pixels = new Integer[height][width];

        for (int x = 0; x < height ; x++) {
            for (int y = 0; y < width; y++) {
                pixels[x][y] = (Integer) (image.getRGB(y, x) == 0xFFFFFFFF ? 0 : 1);
            }
        }
        return pixels;
    }  
    
    public static Component[][] fromPixelArray(Integer[][] pixels){
        int width = pixels.length;
        int length = pixels[0].length;
        Component[][] array = new Component[width][length];
        for(int i=0;i<width;i++){
            for(int j=0; j<length; j++){
                Component comp = new Component(i,j,pixels[i][j]);
                array[i][j] = comp; 
            }
        } 
        return array;
    }
    
    
    public static class MiniComponent{
        public Pos location;
        public String label;
        public Pos down;
        public int id;
        public MiniComponent(int Y, int X, int id){
            this.location = new Pos(Y,X);
            this.id = id;
            this.label = null;
        }
        @Override
        public int hashCode(){
            return location.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MiniComponent other = (MiniComponent) obj;
            if (!Objects.equals(this.location, other.location)) {
                return false;
            }
            return true;
        }
        @Override
        public String toString(){
            String hasDown = "+";
            if(down == null){
                hasDown = "-";
            }
                    
            return location.toString()+hasDown;
        }
    }
    public static class Pos{
        public int y,x;
        public Pos(int Y, int X){
            this.y = Y;
            this.x = X;
        }
        
        @Override
        public String toString(){
            return y+" "+x;
        }
        @Override
        public int hashCode(){
            return y*width + x;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Pos other = (Pos) obj;
            if (this.y != other.y) {
                return false;
            }
            if (this.x != other.x) {
                return false;
            }
            return true;
        }
    }
    public static class Component extends MiniComponent{
        public Pos up;
        public Pos left;
        public Pos right;
        public int c;
        public volatile boolean visited;
        
        public Component(int Y, int X, int id){
            super(Y,X,id);
            this.c = 0;
        }
        @Override
        public String toString(){
            
            return String.valueOf(this.location);
        }
        
    }
    
    
    public static class Shared{
        public Component[][] comp;
        public final int length,width;
        public Shared(Component[][] array){
            this.comp = array;
            this.width = array.length;
            this.length = array[0].length;
        }
        protected Component get(int y, int x){
            if(y<this.width && x<this.length){
                return this.comp[y][x];
            }else{
                return null;
            }
        }
        protected Component get(Pos pos){
            if(pos == null){
                return null;
            }
            return get(pos.y,pos.x);
        }
    }
    public static abstract class Worker extends Shared implements Callable{
        public final int constant;
        public Worker(Component[][] array, int lineOrCol) {
            super(array);
            this.constant = lineOrCol;
        }  
    }   
    public static abstract class Linker extends Worker{
        protected int index;
        public Linker(Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
        }
    }
 
    
    public static class VerticalLinker extends Linker{
        public VerticalLinker(Component[][] array, int col) {
            super(array, col);
        }
        @Override
        public Object call() throws Exception {
            Component prev;
            Component next = this.comp[index][this.constant];
            index++;
            while(index<this.width){
                prev = next;
                next = this.get(index, this.constant);
                if(prev.id == next.id){
                    prev.down = next.location;
                    next.up = prev.location;
                }
                index++;
            }
            return null;
        }

    }
    public static class HorizontalLinker extends Linker{
        public HorizontalLinker(Component[][] array, int line) {
            super(array, line);
        }
        @Override
        public Object call() throws Exception {
            Component prev;
            Component next = this.comp[this.constant][this.index];
            index++;
            while(index<this.length){
                prev = next;
                next = this.get(this.constant, index);
                if(prev.id == next.id){
                    prev.right = next.location;
                    next.left = prev.location;
                }
                index++;
            }
            return null;
        }

    }
    public static Collection<Linker> generateLinkers(Component[][] array){
        int width = array.length;
        int length = array[0].length;
        ArrayList<Linker> linkers = new ArrayList<>(width+length);
        for(int i=0; i<width; i++){
            linkers.add(new HorizontalLinker(array,i));
        }
        for(int j=0; j<length; j++){
            linkers.add(new VerticalLinker(array,j));
        }
        System.out.println(width +" "+length+" "+linkers.size());
        return linkers;
        
    }
    
    
    
    
    
    
    public interface iTableFunction{
        public void onIteration(Object[][] array, int x, int y);
        public void onNewArray(Object[][] array, int x, int y);
    }
    public static void tableFunction(Object[][] array, iTableFunction function){
        for(int i=0;i<array.length;i++){
            for(int j=0; j<array[i].length; j++){
                function.onIteration(array,i,j);
            }
            function.onNewArray(array, i, i);
        } 
    }
    
    public static void start(Collection<? extends Callable> list) throws InterruptedException{
        if(useThreads){
            list.forEach(call ->{
               new Thread(new FutureTask(call)).start();
            });
            return;
        }
        if(pool !=null){
            pool.shutdownNow();
            pool.awaitTermination(1, TimeUnit.DAYS); 
        }
        
        pool = Executors.newFixedThreadPool(THREAD_COUNT);
        list.forEach(t ->{
            pool.submit(t);
        });
    }
    
    public static void join(Collection<? extends Callable> list) throws InterruptedException{
        if(useThreads){
            Thread.sleep(5000);
        }
        if(pool!=null){
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.DAYS);
        }
    }
    
    
    public static Collection<Collection<Component>> massPullOut(Collection<Component> components){
        ArrayList<Collection<Component>> all = new ArrayList<>();
        for(Component c:components){
            Collection<Component> pullOut = pullOut(c);
            if(!pullOut.isEmpty()){
                all.add(pullOut);
            } 
        }
        return all;
    }
    public static Collection<Component> pullOut(Component start){
        
        ArrayList<Component> list = new ArrayList<>();
        if(start!= null && !start.visited){
            start.visited = true;
            list.add(start);
        }else{
            return list;
        }
        
        list.addAll(pullOut(shared.get(start.up)));
        list.addAll(pullOut(shared.get(start.down)));
        list.addAll(pullOut(shared.get(start.left)));
        list.addAll(pullOut(shared.get(start.right)));
        return list;
    }
    public static void simpleRecursiveStrategy(Shared comp){
        List<Component[]> list = Arrays.asList(comp.comp);
        ArrayList<Component> massList = new ArrayList<>();
        for(Component[] carray:list){
            massList.addAll(Arrays.asList(carray));
        }
        Collection<Collection<Component>> massPullOut = massPullOut(massList);

        for(Collection<Component> collection:massPullOut){
            String unusedLabel = getUnusedLabel();
            
            for(Component c:collection){
                c.label = unusedLabel;
            }
            
        }
    
    }
    
    public static void oldStrat(Integer[][] parsePicture) throws Exception{
        ArrayList<Thread> threads = new ArrayList<>();
        Component[][] array = fromPixelArray(parsePicture);
        tableFunction(parsePicture,print);
        shared = new Shared(array);
        System.out.println();
        Collection<Linker> linkers = generateLinkers(array);
        for(Linker link:linkers){
            link.call();
        }
        long time = System.nanoTime();
        threads.clear();
        System.out.println();
        time = System.currentTimeMillis();
        PullerAPI.advancedStrategy(shared);
//        simpleRecursiveStrategy(shared);        
        
//        System.out.println(System.currentTimeMillis()- time);
        System.out.println();
//        tableFunction(array,printY);
        System.out.println();
//        tableFunction(array,printX);
        tableFunction(array,printLabel);
        System.out.println(shared.get(2,4).up);
        System.exit(0);
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception{
        
        String home = "C:/Users/Lemmin/Desktop/";
        String pic = "";
        pic = "Picture2.bmp";
        pic = "large.bmp";
        
        Integer[][] parsePicture = parsePicture(home+pic);
        length = parsePicture[0].length;
        width = parsePicture.length;
//        oldStrat(parsePicture);
        
        optimizedStrategy(parsePicture);
        
    }
    
    
    
    
    
    
    public static iTableFunction print = new iTableFunction() {
            boolean firstPrint = true;
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                if(firstPrint){
                    for(int i=0; i<array[0].length; i++){
                        System.out.print(i%10+" ");
                    }
                    System.out.println();
                    firstPrint = false;
                }
                System.out.print(array[x][y] +" ");
            }

            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                System.out.println(" :"+x);
            }
        };
    public static iTableFunction printLabel = new iTableFunction() {
            boolean firstPrint = true;
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                if(firstPrint){
                    for(int i = 0; i<array[0].length; i++){
                        System.out.print(i%10+" ");
                    }
                    System.out.println();
                    firstPrint = false;
                }
                MiniComponent comp = (MiniComponent)array[x][y];
                String printme = " ";
                if(comp != null && comp.label != null){
                    printme = comp.label;
                }
                System.out.print(printme+" ");
            }
            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                System.out.println(" :"+x);
            }
        };
    public static iTableFunction printX = new iTableFunction() {
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                System.out.print(x +" ");
            }

            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                System.out.println();
            }
        };
    public static iTableFunction printY = new iTableFunction() {
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                System.out.print(y +" ");
            }

            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                System.out.println();
            }
        };
    public static iTableFunction printCase = new iTableFunction() {
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                Component comp = (Component)array[x][y];
                String printme = " ";
                if(comp != null){
                    printme = comp.c + "";
                }
                System.out.print(printme+" ");
            }

            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                System.out.println(" :"+x);
            }
        };
    
    
    public static ArrayList sortByComponent(Collection<MiniComponent> list){
        ArrayList l = new ArrayList(list);
        Collections.sort(l, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                MiniComponent c1,c2;
                c1 = (MiniComponent)o1;
                c2 = (MiniComponent)o2;
                
                return c1.hashCode() - c2.hashCode();
            }
        });
        return l;
    }
    public static ArrayList sortByPos(Collection<Pos> list){  
        ArrayList l = new ArrayList(list);
        Collections.sort(l, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Pos c1,c2;
                c1 = (Pos)o1;
                c2 = (Pos)o2;
                
                return c1.hashCode() - c2.hashCode();
            }
        });
        return l;
    }
    
    
        
    
    
    

    
    
    
    
}
