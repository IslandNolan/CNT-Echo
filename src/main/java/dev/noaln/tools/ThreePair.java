package dev.noaln.tools;

public class ThreePair<L, M, R> {
    private static final long serialVersionUID = 4954918890077093841L;
    public L left;
    public M middle;
    public R right;

    public ThreePair() {}

    public ThreePair(L left, M middle, R right) {
        this.left = left;
        this.right = right;
        this.middle = middle;
    }
    //region getters
    public L getLeft() {
        return this.left;
    }
    public M getMiddle(){
        return this.middle;
    }
    public R getRight() {
        return this.right;
    }
    //endregion


    //region setters
    public void setLeft(L left) {
        this.left = left;
    }
    public void setMiddle(M middle) {
        this.middle = middle;
    }
    public void setRight(R right) {
        this.right = right;
    }
    //endregion

}

