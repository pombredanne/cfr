package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.SetFactory;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lee
 * Date: 22/03/2012
 * Time: 06:45
 * To change this template use File | Settings | File Templates.
 */
public class BooleanOperation extends AbstractExpression implements ConditionalExpression {
    private ConditionalExpression lhs;
    private ConditionalExpression rhs;
    private BoolOp op;

    public BooleanOperation(ConditionalExpression lhs, ConditionalExpression rhs, BoolOp op) {
        super(new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION));
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }

    @Override
    public int getSize() {
        return 2 + lhs.getSize() + 2 + rhs.getSize();
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        lhs = expressionRewriter.rewriteExpression(lhs, ssaIdentifiers, statementContainer, flags);
        rhs = expressionRewriter.rewriteExpression(rhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print("(").dump(lhs).print(") " + op.getShowAs() + " (").dump(rhs).print(")");
    }

    @Override
    public ConditionalExpression getNegated() {
        return new NotOperation(this);
    }

    @Override
    public ConditionalExpression getDemorganApplied(boolean amNegating) {
        return new BooleanOperation(lhs.getDemorganApplied(amNegating), rhs.getDemorganApplied(amNegating), amNegating ? op.getDemorgan() : op);
    }

    @Override
    public Set<LValue> getLoopLValues() {
        Set<LValue> res = SetFactory.newSet();
        res.addAll(lhs.getLoopLValues());
        res.addAll(rhs.getLoopLValues());
        return res;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        lhs.collectUsedLValues(lValueUsageCollector);
        rhs.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public ConditionalExpression optimiseForType() {
        lhs = lhs.optimiseForType();
        rhs = rhs.optimiseForType();
        return this;
    }

    @Override
    public ConditionalExpression simplify() {
        return ConditionalUtils.simplify(this);
    }

}
