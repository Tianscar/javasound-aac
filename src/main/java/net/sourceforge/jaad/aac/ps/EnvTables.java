package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 12:18
 */
class EnvTables {

    final Huffman.Table f;
    final Huffman.Table t;

    EnvTables(Huffman.Table f, Huffman.Table t) {
        this.f = f;
        this.t = t;
    }

    EnvTables(int[][] f, int[][] t) {
        this(Huffman.table(f), Huffman.table(t));
    }

    Huffman.Table table(boolean dt) {
        return dt ? t : f;
    }
}
