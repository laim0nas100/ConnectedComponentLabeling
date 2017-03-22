/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.imageio.ImageIO;

/**
 *
 * @author Lemmin
 */
public class ConnectedComponentLabeling {
    public static ExecutorService pool;
    public static HashSet<Character> set = new HashSet<>();
    public static Shared shared;
    public static Character getUnusedLabel(){
        int i = 33;
        char c = (char)i;
        while(set.contains(c) && i<127){
            i++;
            c = (char)i;
        }
        set.add(c);
        return c;
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
        int length = pixels.length;
        int width = pixels[0].length;
        Component[][] array = new Component[length][width];
        for(int i=0;i<length;i++){
            for(int j=0; j<width; j++){
                Component comp = new Component(i,j,pixels[i][j]);
                array[i][j] = comp; 
            }
        } 
        return array;
    }
    public static class Pos{
        public int x,y;
        public Pos(int X, int Y){
            this.x = X;
            this.y = Y;
        }
        
        @Override
        public String toString(){
            return x+" "+y;
        }
    }
    public static class Component{
        public Pos location;
        public Pos up;
        public Pos down;
        public Pos left;
        public Pos right;
        public int id;
        public Character label;
        public int c;
        
        public Component(int X, int Y, int id){
            this.location = new Pos(X,Y);
            this.id = id;
            this.label = null;
            this.c = 0;
        }
        @Override
        public String toString(){
            
            return String.valueOf(id);
        }
    }
    
    
    public static class Shared{
        public Component[][] comp;
        public final int length,width;
        public Shared(Component[][] array){
            this.comp = array;
            this.length = array.length;
            this.width = array[0].length;
        }
        protected Component get(int x, int y){
            if(x<this.length && y<this.width){
                return this.comp[x][y];
            }else{
                return null;
            }
        }
        protected Component get(Pos pos){
            return get(pos.x,pos.y);
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
//                next = this.comp[index][this.constant];
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
//                next = this.comp[this.constant][this.index];
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
        ArrayList<Linker> linkers = new ArrayList<>();
        for(int i=0; i<width; i++){
            linkers.add(new HorizontalLinker(array,i));
        }
        for(int i=0; i<length; i++){
            linkers.add(new VerticalLinker(array,i));
        }
        return linkers;
        
    }
    
    
    public static abstract class Labeler extends Worker{
        public volatile int index;
        public Labeler dependancy;
        public Labeler(Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
        }
        
    }
    
    public static class CompBlock{
        public Component c1,c2,c3,c4;
        public CompBlock(Component c1,Component c2, Component c3, Component c4){
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.c4 = c4;
        }
        
    }
    
    public static int test(Component c1, Component c2, Component c3){
        
        if(c1.down!=null){// 1 2 5 7 9
            if(c1.right!=null){// 1 2
                if(c2.down!=null){
                    return 1;
                }else if(c3.right==null){
                    return 2;
                }
            }else{// 5 7 9
                if(c3.right!=null && c2.down == null){
                    return 5;
                }else{
                    if(c2.down != null){
                        return 7;
                    }else{
                        return 9;
                    }
                }
                
            }
        }else{// 3 4 6 8 10 11 12
           if(c1.right!=null){// 3 6 10
                if(c3.right== null){
                    if(c2.down != null){
                        return 3;
                    }else{
                        return 10;
                    }
                }else if(c2.down == null){
                    return 6;
                }
                
            }else{// 4 8 11 12
               
               if(c2.down != null){// 4 11
                   if(c3.right != null){
                       return 4;
                   }else{
                       return 11;
                   }
               }else{// 8 10
                   if(c3.right != null){
                       return 8;
                   }else{
                       return 10;
                   }
               }
                
            } 
        }
        return -1;
        
    }
    public static void labelBlock(CompBlock block, int testCase){
        switch(testCase){
                case 1:{
                    block.c2.label = block.c1.label;
                    block.c3.label = block.c1.label;
                    block.c4.label = block.c1.label;
                    break;
                }
                case 2:{
                    block.c2.label = block.c1.label;
                    block.c3.label = block.c1.label;
//                    if(block.c4.label == null){
                        block.c4.label = getUnusedLabel();
//                    }
                    break;
                }
                case 3:{
                    block.c2.label = block.c1.label;
                    if(block.c3.label == null){
                        block.c3.label = getUnusedLabel();
                    }
                    block.c4.label = block.c1.label;
                    break;
                }
                case 4:{
                    if(block.c2.label == null){
                        block.c2.label = getUnusedLabel();    
                    }
                    
                    block.c3.label = block.c2.label;
                    block.c4.label = block.c2.label;
                    break;
                }
                case 5:{
                    if(block.c2.label == null){
                       block.c2.label = getUnusedLabel(); 
                    }
                    
                    block.c3.label = block.c1.label;
                    block.c4.label = block.c1.label;
                    break;
                }
                case 6:{
                    block.c2.label = block.c1.label;
                    if(block.c3.label == null){
                        block.c3.label = getUnusedLabel();
                    }
                    
                    block.c4.label = block.c3.label;
//                    System.out.println(6);
                    break;
                }
                case 7:{
                    if(block.c2.label == null){
                        block.c2.label = getUnusedLabel();
                    }
                    block.c3.label = block.c1.label;
                    block.c4.label = block.c2.label;
                    break;
                }
                case 8:{
                    if(block.c2.label == null){
                        block.c2.label = getUnusedLabel();
                    }
                    block.c3.label = getUnusedLabel();
                    block.c4.label = block.c3.label;
//                    System.out.println(8);
                    break;
                }
                case 9:{
                    if(block.c2.label == null){
                        block.c2.label = getUnusedLabel();
                    }
                    block.c3.label = block.c1.label;
                    block.c4.label = getUnusedLabel();
//                    System.out.println(9);
                    break;
                }
                case 10:{
                    block.c2.label = block.c1.label;
                    if(block.c3.label == null){
                        block.c3.label = getUnusedLabel();  
                    }
                    if(block.c4.label == null){
                        block.c4.label = getUnusedLabel();
                    }
                    
//                    System.out.println(10);
                    break;
                }
                case 11:{
//                    block.c2.label = getUnusedLabel();
                    if(block.c3.label == null){
                        block.c3.label = getUnusedLabel();  
                    }
                    block.c4.label = block.c2.label;
//                    System.out.println(11);
                    break;
                }
                case 12:{
                    block.c2.label = getUnusedLabel();
                    block.c3.label = getUnusedLabel();
                    block.c4.label = getUnusedLabel();
//                    System.out.println(12);
                    break;
                }
        }
    }
    public static class HorizontalLabeler extends Labeler{
        public int consInc;
        private HashMap<Character,ArrayList<Component>> labels = new HashMap<>();
        public HorizontalLabeler(Component[][] array, int lineOrCol) {
            super(array, lineOrCol);
            consInc = constant+1;
        }
        private void putToHash(Component c){
            if(labels.containsKey(c.label)){
                labels.get(c.label).add(c);
            }else{
                ArrayList<Component> list = new ArrayList<>();
                list.add(c);
                labels.put(c.label, list);
            }
        }
        @Override
        public Object call() throws Exception {
            Component c;
            while(index<this.length-1){
                if(this.dependancy !=null){
                    while(index>=dependancy.index){}
                }
                c = get(constant,index);
                
                Component down = get(consInc,index);
                Component right = get(constant,index+1);
                Component downRight = get(consInc,index+1);
                CompBlock block = new CompBlock(c,right,down,downRight);
                int testCase = test(c,right,down);
                c.c = testCase;
                labelBlock(block,testCase);
                putToHash(c);
                putToHash(down);
                putToHash(right);
                putToHash(downRight);
                
                //test collision
                if(constant>0){
                    Component up = get(constant-1,index);
                    if(up== null){
                        index++;
                        continue;
                    }
                    if(c.up != null && (!Objects.equals(c.label, up.label))){
//                        Character la = up.label;
                        System.out.println("Collision up "+ c.location.toString());
//                        ArrayList<Component> relabelThese = this.labels.remove(c.label);
//                        for(Component co:relabelThese){
//                            System.out.print(co.label +"<-"+la+", ");
//                            co.label = la;
//                        }
//                        System.out.println();
//                        this.labels.put(la, relabelThese);
//                        if(!Objects.equals(c.label, up.label)){
//                            c.label = la;
//                        }
                        Component next = c;
                        Character ch = c.label;
                        do{
                            next.label = ch;
                            if(next.up == null){
                                break;
                            }
                            next = get(next.up);
                        }while(next!= null);
                    }
                }
                if(index>0){
                    Component left = get(constant,index-1);
                    if(left== null){
                        index++;
                        continue;
                    }
                    if(c.left != null && (!Objects.equals(c.label, left.label))){
//                        Character la = c.label;
                        System.out.println("Collision left "+ c.location.toString());
//                        ArrayList<Component> relabelThese = this.labels.remove(left.label);
//                        System.out.println("Relabel "+relabelThese.size());
//                        for(Component co:relabelThese){
//                            System.out.print(co.label +"<-"+la+", ");
//                            co.label = la;
//                        }
//                        System.out.println();
//                        this.labels.put(la, relabelThese);

                        Component next = c;
                        Character ch = c.label;
                        do{
                            next.label = ch;
                            if(next.left == null){
                                break;
                            }
                            next = get(next.left);
                        }while(next!= null);
                    }
                }
                
                
                index++;
                
            }
            index+=100;
            return null;
        }
        
    }
    
       
    public static Collection<Labeler> generateLabelers(Component[][] array){
        ArrayList<Labeler> labelers = new ArrayList<>();
        array[0][0].label = getUnusedLabel();
        HorizontalLabeler dependency = null;
        for(int i=0; i<shared.width-1; i++){
            HorizontalLabeler labeler = new HorizontalLabeler(array,i);
            labeler.dependancy = dependency;
            labelers.add(labeler);
            dependency = labeler;
        }
        return labelers;
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
    
   
    
    
    
    public static void start(Collection<Thread> list){
        list.forEach(t ->{
            t.start();
        });
    }
    
    public static void join(Collection<Thread> list) throws InterruptedException{
       for(Thread t:list){
           t.join();
       }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception{
//        pool = Executors.newFixedThreadPool(2);
        ArrayList<Thread> threads = new ArrayList<>();
        String home = "C:/Users/Lemmin/Desktop/";
        iTableFunction print = new iTableFunction() {
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                System.out.print(array[x][y] +" ");
            }

            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                System.out.println(" :"+x);
            }
        };
        iTableFunction printLabel = new iTableFunction() {
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                Component comp = (Component)array[x][y];
                System.out.print(comp.label+" ");
            }
            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                System.out.println(" :"+x);
            }
        };
        iTableFunction printCase = new iTableFunction() {
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                Component comp = (Component)array[x][y];
                System.out.print(comp.c+" ");
            }

            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                System.out.println(" :"+x);
            }
        };
        Integer[][] parsePicture = parsePicture(home+"Picture.bmp");
        
        Component[][] array = fromPixelArray(parsePicture);
        tableFunction(array,print);
        shared = new Shared(array);
        Collection<Linker> linkers = generateLinkers(array);
        long time = System.nanoTime();
        linkers.forEach(task ->{
            threads.add(new Thread(new FutureTask(task)));
        });
        start(threads);
        join(threads);
        threads.clear();
//        pool.shutdown();
//        pool.awaitTermination(1, TimeUnit.DAYS);
        System.out.println(array[0][0].down);
        Collection<Labeler> labelers = generateLabelers(array);
        labelers.forEach(task ->{
            threads.add(new Thread(new FutureTask(task)));
        });
        start(threads);
        join(threads);
        tableFunction(array,printLabel);
        System.out.println(System.nanoTime() - time);
        System.out.println(set);
        System.out.println(shared.length+" "+shared.width);
        tableFunction(array,printCase);
        Component comp = shared.get(15,15);
        System.out.println(comp.label);
        System.out.println(comp.down + " "+ comp.up);
        
    }
    
    
    
    
    
    
}
