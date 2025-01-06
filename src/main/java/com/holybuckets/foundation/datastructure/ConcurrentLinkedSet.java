package com.holybuckets.foundation.datastructure;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentLinkedSet<T> implements Set<T> {
    
    private final ConcurrentHashMap<T, Node<T>> map;
    private final Node<T> header;
    
    private static class Node<T> {
        T element;
        Node<T> next;
        Node<T> prev;
        
        Node(T element, Node<T> next, Node<T> prev) {
            this.element = element;
            this.next = next;
            this.prev = prev;
        }
    }
    
    public ConcurrentLinkedSet() {
        map = new ConcurrentHashMap<>();
        header = new Node<>(null, null, null);
        header.next = header.prev = header;
    }

    public ConcurrentLinkedSet(int initialCapacity) {
        map = new ConcurrentHashMap<>(initialCapacity);
        header = new Node<>(null, null, null);
        header.next = header.prev = header;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = header.next;
            private Node<T> lastReturned = null;
            
            @Override
            public boolean hasNext() {
                return current != header;
            }
            
            @Override
            public T next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                lastReturned = current;
                current = current.next;
                return lastReturned.element;
            }
            
            @Override
            public void remove() {
                if (lastReturned == null)
                    throw new IllegalStateException();
                ConcurrentLinkedSet.this.remove(lastReturned.element);
                lastReturned = null;
            }
        };
    }

    @Override
    public Object[] toArray() {
        List<T> list = new ArrayList<>();
        for (T element : this) {
            list.add(element);
        }
        return list.toArray();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        List<T> list = new ArrayList<>();
        for (T element : this) {
            list.add(element);
        }
        return list.toArray(a);
    }

    @Override
    public boolean add(T t) {
        if (map.containsKey(t))
            return false;
            
        Node<T> newNode = new Node<>(t, header, header.prev);
        synchronized (header) {
            header.prev.next = newNode;
            header.prev = newNode;
        }
        map.put(t, newNode);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        Node<T> node = map.remove(o);
        if (node != null) {
            synchronized (header) {
                node.prev.next = node.next;
                node.next.prev = node.prev;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean modified = false;
        for (T e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean modified = false;
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object e : c) {
            if (remove(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        synchronized (header) {
            header.next = header.prev = header;
        }
        map.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Set))
            return false;
        Set<?> s = (Set<?>) o;
        return s.size() == map.size() && containsAll(s);
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (T e : this) {
            if (e != null) {
                h += e.hashCode();
            }
        }
        return h;
    }
}
