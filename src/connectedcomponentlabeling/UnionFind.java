/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connectedcomponentlabeling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
 
/**
 * This class is to provide a data structure for disjoint sets.
 * It supports basic <tt>union</tt> and <tt>find</tt> operations.
 * 
 * This data structure is implemented by using a backing <tt>Map</tt>, 
 * nevertheless, it can be implemented by using a backing <tt>Array</tt> or <tt>Set</tt>.
 * 
 * The upside is the fact the using a <tt>Map</tt> can significantly decrease the running time 
 * of the <tt>getNode</tt> operation which is often used to get the reference of the node by giving the object itself.
 * However, the downside is using a backing map will increase the memory usage since the map keep tracks of 
 * the nodes and corresponding objects.
 * 
 * Because the backing data structure is a <tt>Map</tt>, so this class does not allow duplicate objects
 * 
 * Some of the codes referenced Introduction to Algorithms by Cormen et al.
 */
public class UnionFind<T> {
    public class UnionFindNode<T> {
        public UnionFindNode<T> parent;

        public T data;
        //An extra attribute to provide the union-by-rank feature
        public int rank;

        public UnionFindNode(UnionFindNode<T> p, T d){
            parent = p;
            data = d;
            rank = 0;
        }
        /**
         * To create a root node, whose parent is itself
         * @param d The data embedded in the node
         */
        public UnionFindNode(T d){
            parent = this;
            data = d;
            rank = 0;
        }
    }
    
    public Map<T, UnionFindNode<T>> map;
    /**
     * Default constructor
     */
    public UnionFind(){
        map = new HashMap<T, UnionFindNode<T>>();
    }
    public UnionFind(Collection<? extends T> input){
        this();
        for(Object o: input){
            makeSet((T) o);
        }        
    }
    /**
     * Internal method for <tt>add</tt> to create a new <tt>UnionFindNode</tt>
     * @param data
     */
    private void makeSet(T data){
        UnionFindNode<T> root = new UnionFindNode<T>(data);
        root.rank = 0;
        map.put(data, root);
    }
    /**
     * Add a new set to this structure
     * @param data An object put in the set
     */
    public void add(T data){
        makeSet(data);
    }
    /**
     * This method returns the root representative of the given object
     * @param target Target object
     * @return The representative of the set which contains the target object
     */
    public T find(T target){
        return find(getNode(target)).data;
    }
    /**
     * Union two sets
     * @param t1
     * @param t2
     */
    public void union(T t1, T t2){
        union(getNode(t1), getNode(t2));
    }
    /**
     * Internal method to perform the <tt>find</tt> operation
     * @param target Given object
     * @return The representative of the set which contains the given object
     */
    private UnionFindNode<T> find(UnionFindNode<T> target){
        if(target != target.parent)
            target.parent = find(target.parent);
        return target.parent;
    }
    /**
     * Link two node by applying <i>path compression</i> and <i>union-by-rank</i> strategies.
     * The order of the two arguments does not matter.
     * @param x Node 1
     * @param y Node 2
     */
    private void link(UnionFindNode<T> x, UnionFindNode<T> y){
        if(x.rank > y.rank){
            //link y to x
            y.parent = x;
        }else if(x.rank < y.rank){
            //link x to y
            x.parent = y;
        }else{
            y.parent = x;
            x.rank++;
        }
    }
    /**
     * Internal method to perform <tt>union</tt> operaion by calling <tt>link</tt>
     * function on the representative nodes of the two given node.
     * @param x Node 1
     * @param y Node 2
     */
    private void union(UnionFindNode<T> x, UnionFindNode<T> y){
        if(x==y)
            throw new IllegalArgumentException("You can not union only 1 data");
        link(find(x), find(y));
    }
    /**
     * This method is to search and return the node based on the given object by iterating the backing set
     * @param o The object to be found
     * @return The node which contains the object
     * @throws NoSuchElementException if the backing map doesn't containt the object
     */
    private UnionFindNode<T> getNode(T o){
        UnionFindNode<T> node = map.get(o);
        if(node==null)
            throw new NoSuchElementException();
        return node;
    }
    /**
     * Check if there is a path that connects all the nodes to a single root
     * @return true if the statement above exists; false otherwise
     */
    public boolean checkSinglePathContainAllNode(){
        //make a copy of the backing set for not compromising the original data
        Set<UnionFindNode<T>> copy = new HashSet<UnionFindNode<T>>();
        copy.addAll(map.values());
        int check = 0;
        for(UnionFindNode<T> node: copy){
            if(node.parent == node){
                if(++check > 1)
                    return false;
            }
        }
        return true;
    }
    /**
     * Given a data, show the path from the start point to its root
     * 
     * @param data
     * @return
     */
    public List<T> findPath(T data){
        List<T> path = new ArrayList<T>();
        UnionFindNode<T> start = getNode(data);
        while(start != start.parent){
            path.add(start.data);
            start = start.parent;
        }
        path.add(find(start).data);
        return path;
    }
}
