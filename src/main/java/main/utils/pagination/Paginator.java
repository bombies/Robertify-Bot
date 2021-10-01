package main.utils.pagination;

import lombok.Getter;

public class Paginator {
    @Getter
    final String front, previous, next, end;

    public Paginator(String front, String previous, String next, String end) {
        this.front = front;
        this.previous = previous;
        this.next = next;
        this.end = end;
    }
}
