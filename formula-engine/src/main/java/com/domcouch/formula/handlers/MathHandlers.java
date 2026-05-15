package com.domcouch.formula.handlers;

import com.domcouch.formula.Evaluator;
import com.domcouch.formula.Expr;
import com.domcouch.formula.FunctionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Math @Function handlers.
 */
public final class MathHandlers {

    private MathHandlers() {}

    public static void register(java.util.Map<String, FunctionHandler> functions) {
        functions.put("ABS", (ev, args, ctx) -> {
            Object val = ev.eval(args.getFirst(), ctx);
            List<Object> sources = Evaluator.toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) result.add(Math.abs(Evaluator.toNumber(src)));
            return result.size() == 1 ? result.getFirst() : result;
        });
        functions.put("ACOS", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::acos));
        functions.put("ASIN", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::asin));
        functions.put("ATAN", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::atan));
        functions.put("ATAN2", (ev, args, ctx) -> Evaluator.map2(ev, args, ctx, Math::atan2));
        functions.put("COS", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::cos));
        functions.put("SIN", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::sin));
        functions.put("TAN", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::tan));
        functions.put("EXP", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::exp));
        functions.put("LOG", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::log10));
        functions.put("SQRT", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::sqrt));
        functions.put("PI", (ev, args, ctx) -> Math.PI);
        functions.put("POWER", (ev, args, ctx) -> Evaluator.map2(ev, args, ctx, Math::pow));
        functions.put("INTEGER", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, v -> (double) (long) v));
        functions.put("ROUND", (ev, args, ctx) -> {
            Object val = ev.eval(args.get(0), ctx);
            double factor = args.size() > 1 ? Evaluator.toNumber(ev.eval(args.get(1), ctx)) : 1.0;
            List<Object> sources = Evaluator.toList(val);
            List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                double v = Evaluator.toNumber(src);
                result.add(Math.round(v / factor) * factor);
            }
            return result.size() == 1 ? result.getFirst() : result;
        });
        functions.put("SIGN", (ev, args, ctx) -> {
            double v = Evaluator.toNumber(ev.eval(args.getFirst(), ctx));
            return v > 0 ? 1.0 : v < 0 ? -1.0 : 0.0;
        });
        functions.put("MODULO", (ev, args, ctx) -> Evaluator.map2(ev, args, ctx, (a, b) -> {
            if (b == 0) return 0.0;
            double r = a % b;
            return r < 0 ? r + Math.abs(b) : r;
        }));
        functions.put("FLOATEQ", (ev, args, ctx) -> {
            double a = Evaluator.toNumber(ev.eval(args.get(0), ctx));
            double b = Evaluator.toNumber(ev.eval(args.get(1), ctx));
            double eps = args.size() > 2 ? Evaluator.toNumber(ev.eval(args.get(2), ctx)) : 1e-15;
            return Evaluator.boolToNum(Math.abs(a - b) <= eps);
        });
        functions.put("LN", (ev, args, ctx) -> Evaluator.map1(ev, args, ctx, Math::log));
        functions.put("MAX", (ev, args, ctx) -> {
            double max = Double.NEGATIVE_INFINITY;
            for (Expr arg : args) for (Object o : Evaluator.toList(ev.eval(arg, ctx))) max = Math.max(max, Evaluator.toNumber(o));
            return max == (int) max ? (double) (int) max : max;
        });
        functions.put("MIN", (ev, args, ctx) -> {
            double min = Double.POSITIVE_INFINITY;
            for (Expr arg : args) for (Object o : Evaluator.toList(ev.eval(arg, ctx))) min = Math.min(min, Evaluator.toNumber(o));
            return min == (int) min ? (double) (int) min : min;
        });
        functions.put("SUM", (ev, args, ctx) -> {
            double sum = 0;
            for (Expr arg : args) for (Object o : Evaluator.toList(ev.eval(arg, ctx))) sum += Evaluator.toNumber(o);
            return sum == (int) sum ? (double) (int) sum : sum;
        });
    }
}
