/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import LibraryLB.Log;
import connectedcomponentlabeling.OptimizedAPI.MiniShared;
import static connectedcomponentlabeling.OptimizedAPI.optimizedStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 *
 * @author Lemmin
 */
public class ConnectedComponentLabeling {
    public static final int THREAD_COUNT = 4;
    public static ExecutorService pool;
    public static HashSet<String> set = new HashSet<>();
    public static MiniShared shared;
    public static int charInt = 33;
    public static final int CORE_COUNT = Runtime.getRuntime().availableProcessors();
    public static MiniComponent[][] transpose(MiniComponent[][] array) throws InterruptedException{
        
        ExecutorService service = Executors.newFixedThreadPool(CORE_COUNT);
        final int size = array[0].length;
        final MiniComponent[][] result = new MiniComponent[size][array.length];
        for(int i=0;i<array.length; i++){
            final int index = i;
            Runnable run = () ->{
                for(int j=0; j<size; j++){
                    result[j][index] = array[index][j];
                }
            };
            service.submit(run);
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);
        return result;
        
    }
    public static String getUnusedLabel(){
        
        charInt++;
        String valueOf = String.valueOf(Character.toChars(charInt));
        return valueOf;
    }
    public static Integer[][] parsePicture(String path,boolean monochrome) throws IOException{
        BufferedImage image = ImageIO.read(new File(path));
        int h = image.getHeight();
        int w = image.getWidth();
        Integer[][] pixels = new Integer[h][w];

        for (int x = 0; x < h ; x++) {
            for (int y = 0; y < w; y++) {
                if(!monochrome){
                    pixels[x][y] = (Integer) (image.getRGB(y, x)); 
                }else{
                    pixels[x][y] = (Integer) (image.getRGB(y, x) == 0xFFFFFFFF ? 0 : 1);

                }
                
            }
        }
        return pixels;
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
            return y*shared.width() + x;
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
    
    public static void start(Collection<? extends Callable> list) throws InterruptedException, Exception{
        join();
        
        pool = Executors.newFixedThreadPool(THREAD_COUNT);
        for(Callable c:list){
            pool.submit(c);
        }
        
        
    }
    
    public static void join() throws InterruptedException{
        if(pool!=null){
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.DAYS);
        }
    }
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception{
        String home = "";
        home = "C:/Users/Lemmin/Desktop/";
        home = "/mnt/Extra-Space/Dev/Java/Workspace/ConnectedComponentLabeling/test/";
        String pic = "";
//        pic = "bars.bmp";
        pic = "picture.bmp";
//        pic = "large.bmp";
//        pic = "PictureStrat.png";
//        pic = "color.bmp";
        pic = "img.bmp";
//        Log.changeStream('f', "res.txt");
        Log.timeStamp = false;
        Log.println("Core count "+CORE_COUNT);
        Integer[][] parsePicture = parsePicture(home+pic,false);
        
        
        {
            shared = new MiniShared(OptimizedAPI.fromPixelArrayMini(parsePicture));
            BufferedImage image = optimizedStrategy(shared,false,true,false);
            ImageIO.write(image, "png", new File(home+"result.png"));
        }
//        {
//            shared = new MiniShared(OptimizedAPI.fromPixelArrayMini(parsePicture));
//            BufferedImage image = optimizedStrategy(shared,true,true,false);
//            ImageIO.write(image, "png", new File(home+"result2.png"));
//            
//        }
        Log.close();
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
            String line = "";
            @Override
            public void onIteration(Object[][] array, int x, int y) {
                if(firstPrint){
                    String numbers = "";
                    for(int i = 0; i<array[0].length; i++){
                        numbers += i%10;
                    }
                    Log.println(numbers);
                    firstPrint = false;
                }
                MiniComponent comp = (MiniComponent)array[x][y];
                String printme = " ";
                if(comp != null && comp.label != null){
                    printme = comp.label;
                }
                line+= printme;
            }
            @Override
            public void onNewArray(Object[][] array, int x, int y) {
                Log.println(line+" :"+x);
                line = "";
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
