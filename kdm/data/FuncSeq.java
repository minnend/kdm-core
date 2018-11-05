package kdm.data;

/** Represents a function of a sequence: y_{1..T}=f(x_{1..T}), where x and y are sequences of vectors */
public interface FuncSeq
{
   public Sequence compute(Sequence data);
}
