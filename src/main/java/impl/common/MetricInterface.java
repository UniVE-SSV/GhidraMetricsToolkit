package impl.common;

public interface MetricInterface<T extends ResultInterface> {
    T compute();
}
