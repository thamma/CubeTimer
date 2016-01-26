package sample;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Dominic on 12/21/2015.
 */
public abstract class DFA {
    public List<Node> nodes;
    public Node current;

    public DFA(Node... nodes) {
        this.nodes = new ArrayList<Node>();
        this.nodes.addAll(Arrays.asList(nodes));
        this.current = this.nodes.get(0);
    }

    public void read(String s) {
        s = "" + s.charAt(0);
        final String finalS = s;
        if (current.outgoing.stream().filter(out -> out.first.equals(finalS)).count() != 0) {
            String newNode = current.outgoing.stream().filter(out -> out.first.equals(finalS)).findFirst().get().second;
            current = this.nodes.stream().filter(n -> n.key.equals(newNode)).findFirst().get();
        }
        Platform.runLater(() -> updateTask());

    }

    public abstract void updateTask();

    public boolean accepts(String in) {
        Node tempCurrent = this.nodes.get(0);
        for (char c : in.toCharArray()) {
            String s = "" + c;
            if (tempCurrent.outgoing.stream().filter(out -> out.first.equals(s)).count() != 0) {
                String newNode = tempCurrent.outgoing.stream().filter(out -> out.first.equals(s)).findFirst().get().second;
                tempCurrent = this.nodes.stream().filter(n -> n.key.equals(newNode)).findFirst().get();
            }
        }
        return tempCurrent.accept;
    }
}

class Node {

    public String key;
    public List<Tuple<String>> outgoing;
    public boolean accept;

    public Node(String key, Tuple<String>... outgoing) {
        this(key, Arrays.asList(outgoing), false);
    }

    public Node(String key, boolean accept, Tuple<String>... outgoing) {
        this(key, Arrays.asList(outgoing), accept);
    }


    public Node(String key, List<Tuple<String>> outgoing, boolean accept) {
        this.key = key;
        this.outgoing = outgoing;
        this.accept = accept;
    }
}

class Tuple<T> {
    public T first, second;

    public Tuple(T t1, T t2) {
        this.first = t1;
        this.second = t2;
    }
}