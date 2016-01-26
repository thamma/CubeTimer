package sample;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dominic on 12/21/2015.
 */
public class Session extends ArrayList<Long> {


    public long date;

    public Session(long date) {
        this.date = date;
    }
}
