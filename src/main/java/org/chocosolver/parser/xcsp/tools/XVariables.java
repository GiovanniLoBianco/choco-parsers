package org.chocosolver.parser.xcsp.tools;

import org.chocosolver.parser.xcsp.tools.XDomains.Dom;
import org.chocosolver.parser.xcsp.tools.XValues.IntegerEntity;
import org.chocosolver.parser.xcsp.tools.XValues.IntegerInterval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * In this class, we find intern classes for managing variables and arrays of variables.
 */
public class XVariables {

    public static final String OTHERS = "others";

    /**
     * The enum type describing the different types of variables.
     */
    public static enum TypeVar {
        integer,
        symbolic,
        real,
        stochastic,
        symbolic_stochastic,
        set,
        symbolic_set,
        undirected_graph,
        directed_graph,
        point,
        interval,
        region;

        public boolean isStochastic() {
            return this == stochastic || this == symbolic_stochastic;
        }

        /**
         * Returns true if the constant corresponds to integer, symbolic, real or (symbolic) stochastic.
         */
        public boolean isBasic() {
            return this == integer || this == symbolic || this == real || isStochastic();
        }

        public boolean isSet() {
            return this == set || this == symbolic_set;
        }

        public boolean isGraph() {
            return this == undirected_graph || this == directed_graph;
        }

        public boolean isComplex() {
            return isSet() || isGraph();
        }

        public boolean isQualitative() {
            return this == point || this == interval || this == region;
        }
    }

    /**
     * The root class used for Var and Array objects.
     */
    public static abstract class Entry {
        /**
         * The id (unique identifier) of the entry.
         */
        public final String id;

        /**
         * The type of the entry.
         */
        public final TypeVar type;

        /**
         * Builds an entry with the specified id and type.
         */
        protected Entry(String id, TypeVar type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public String toString() {
            return id + ":" + type;
        }
    }

    /**
     * The class used to represent variables.
     */
    public static final class Var extends Entry {
        /**
         * The domain of the variable. It is null if the variable is qualitative.
         */
        public final Dom dom;

        /**
         * The degree of the variable. This is automatically computed after all constraints have been parsed.
         */
        public int degree;

        /**
         * Builds a variable with the specified id, type and domain.
         */
        protected Var(String id, TypeVar type, Dom dom) {
            super(id, type);
            this.dom = dom;
        }

        /**
         * Builds a variable from an array with the specified id (combined with the specified indexes), type and domain.
         */
        protected Var(String idArray, TypeVar type, Dom dom, int[] indexes) {
            this(idArray + "[" + XUtility.join(indexes, "][") + "]", type, dom);
        }

        @Override
        public String toString() {
            return id; // + " :" + type + " of " + dom;
        }
    }

    /**
     * The class used to represent arrays of variables.
     */
    public static final class Array extends Entry {
        /**
         * The size of the array, as defined in XCSP3.
         */
        public final int[] size;

        /**
         * The flat (one-dimensional) array composed of all variables contained in the (multi-dimensional) array. This way, we can easily deal with arrays of
         * any dimensions.
         */
        public final Var[] vars;

        /**
         * Builds an array of variables with the specified id, type and size.
         */
        protected Array(String id, TypeVar type, int[] size) {
            super(id, type);
            this.size = size;
            this.vars = new Var[Arrays.stream(size).reduce(1, (s, t) -> s * t)];
        }

        /**
         * Builds a variable with the specified domain for each unoccupied cell of the flat array.
         */
        private void buildVarsWith(Dom dom) {
            int[] indexes = new int[size.length];
            for (int i = 0; i < vars.length; i++) {
                if (vars[i] == null)
                    vars[i] = new Var(id, type, dom, indexes);
                for (int j = indexes.length - 1; j >= 0; j--)
                    if (++indexes[j] == size[j])
                        indexes[j] = 0;
                    else
                        break;
            }
        }

        /**
         * Builds an array of variables with the specified id, type and size. All variables are directly defined with the specified domain.
         */
        protected Array(String id, TypeVar type, int[] sizes, Dom dom) {
            this(id, type, sizes);
            buildVarsWith(dom);
        }

        /**
         * Transforms a flat index into a multi-dimensional index.
         */
        protected int[] indexesFor(int flatIndex) {
            int[] t = new int[size.length];
            for (int i = t.length - 1; i > 0; i++) {
                t[i] = flatIndex % size[i];
                flatIndex = flatIndex / size[i];
            }
            t[0] = flatIndex;
            return t;
        }

        /**
         * Transforms a multi-dimensional index into a flat index.
         */
        private int flatIndexFor(int... indexes) {
            int sum = 0;
            for (int i = indexes.length - 1, nb = 1; i >= 0; i--) {
                sum += indexes[i] * nb;
                nb *= size[i];
            }
            return sum;
        }

        /**
         * Returns the variable at the position given by the multi-dimensional index.
         */
        public Var varAt(int... indexes) {
            return vars[flatIndexFor(indexes)];
        }

        /**
         * Builds an array of IntegerEnity objects for representing the ranges of indexes that are computed with respect to the specified compact form.
         */
        public IntegerEntity[] buildIndexRanges(String compactForm) {
            IntegerEntity[] t = new IntegerEntity[size.length];
            String suffix = compactForm.substring(compactForm.indexOf("["));
            for (int i = 0; i < t.length; i++) {
                int pos = suffix.indexOf("]");
                String tok = suffix.substring(1, pos);
                t[i] = tok.length() == 0 ? new IntegerInterval(0, size[i] - 1) : IntegerEntity.parse(tok);
                suffix = suffix.substring(pos + 1);
            }
            return t;
        }

        /**
         * Computes the next multi-dimensional index with respect to specified ranges. Returns false if non exists.
         */
        private boolean incrementIndexes(int[] indexes, IntegerEntity[] indexRanges) {
            int j = indexes.length - 1;
            for (; j >= 0; j--)
                if (indexRanges[j].isSingleton())
                    continue;
                else if (++indexes[j] > ((IntegerInterval) indexRanges[j]).sup)
                    indexes[j] = (int) ((IntegerInterval) indexRanges[j]).inf;
                else
                    break;
            return j >= 0;
        }

        /**
         * Any variable that matches one compact form present in the specified string is built with the specified domain.
         */
        protected void setDom(String s, Dom dom) {
            if (s.trim().equals(OTHERS))
                buildVarsWith(dom);
            else
                for (String tok : s.split("\\s+")) {
                    XUtility.control(tok.substring(0, tok.indexOf("[")).equals(id), "One value of attribute 'for' incorrect in array " + id);
                    IntegerEntity[] indexRanges = buildIndexRanges(tok);
                    int[] indexes = Stream.of(indexRanges).mapToInt(it -> (int) it.smallest()).toArray(); // first index
                    do {
                        int flatIndex = flatIndexFor(indexes);
                        XUtility.control(vars[flatIndex] == null, "Problem with two domain definitions for the same variable");
                        vars[flatIndex] = new Var(id, type, dom, indexes);
                    } while (incrementIndexes(indexes, indexRanges));
                }
        }

        /**
         * Returns the list of variables that match the specified compact form. For example, for x[1..3], the list will contain x[1] x[2] and x[3].
         */
        protected List<Var> getVarsFor(String compactForm) {
            List<Var> list = new ArrayList<>();
            IntegerEntity[] indexRanges = buildIndexRanges(compactForm);
            int[] indexes = Stream.of(indexRanges).mapToInt(it -> (int) it.smallest()).toArray(); // first index
            do {
                list.add(vars[flatIndexFor(indexes)]);
            } while (incrementIndexes(indexes, indexRanges));
            return list;
        }

        @Override
        public String toString() {
            return super.toString() + " [" + XUtility.join(size, "][") + "] " + XUtility.join(vars, " ");
        }
    }
}