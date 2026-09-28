package com.domcouch.formula;

/**
 * Control-flow signal that unwinds the whole formula evaluation. Unlike an error,
 * it is never caught by {@code @IfError} or other error handling inside the formula.
 * <p>
 * {@code @Return} uses it ({@link Evaluator.ReturnValue}). Hosts subclass it for their
 * own interrupts, e.g. a UI runtime that pauses a button formula at {@code @Prompt}
 * until the user has answered, then evaluates the formula again:
 *
 * <pre>{@code
 * class NeedsInput extends FormulaInterrupt {
 *     final PromptRequest request;
 *     NeedsInput(PromptRequest request) { super("waiting for @Prompt"); this.request = request; }
 * }
 * translator.registerFunction("@Prompt", (ev, args, ctx) -> {
 *     if (answers.hasNext()) return answers.next();
 *     throw new NeedsInput(PromptRequest.from(ev, args, ctx));
 * });
 * }</pre>
 *
 * Stack traces are not recorded: this is control flow, not a failure.
 */
public class FormulaInterrupt extends RuntimeException {

    public FormulaInterrupt(String message) {
        super(message, null, false, false);
    }
}
