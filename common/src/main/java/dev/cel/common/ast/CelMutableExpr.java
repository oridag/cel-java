// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dev.cel.common.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import dev.cel.common.ast.CelExpr.CelNotSet;
import dev.cel.common.ast.CelExpr.ExprKind;
import dev.cel.common.ast.CelExpr.ExprKind.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * An abstract representation of a common expression that allows mutation in any of its properties.
 * The expressions are semantically the same as that of the immutable {@link CelExpr}.
 *
 * <p>This allows for an efficient optimization of an AST without having to traverse and rebuild the
 * entire tree.
 *
 * <p>This class is not thread-safe by design.
 */
public final class CelMutableExpr {
  private long id;
  private ExprKind.Kind exprKind;
  private CelNotSet notSet;
  private CelConstant constant;
  private CelMutableIdent ident;
  private CelMutableSelect select;
  private CelMutableCall call;
  private CelMutableCreateList createList;
  private int hash = 0;

  public long id() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public ExprKind.Kind getKind() {
    return exprKind;
  }

  public CelNotSet notSet() {
    checkExprKind(Kind.NOT_SET);
    return notSet;
  }

  public CelConstant constant() {
    checkExprKind(Kind.CONSTANT);
    return constant;
  }

  public CelMutableIdent ident() {
    checkExprKind(Kind.IDENT);
    return ident;
  }

  public CelMutableSelect select() {
    checkExprKind(Kind.SELECT);
    return select;
  }

  public CelMutableCall call() {
    checkExprKind(Kind.CALL);
    return call;
  }

  public CelMutableCreateList createList() {
    checkExprKind(Kind.CREATE_LIST);
    return createList;
  }

  public void setConstant(CelConstant constant) {
    this.exprKind = ExprKind.Kind.CONSTANT;
    this.constant = checkNotNull(constant);
  }

  public void setIdent(CelMutableIdent ident) {
    this.exprKind = ExprKind.Kind.IDENT;
    this.ident = checkNotNull(ident);
  }

  public void setSelect(CelMutableSelect select) {
    this.exprKind = ExprKind.Kind.SELECT;
    this.select = checkNotNull(select);
  }

  public void setCall(CelMutableCall call) {
    this.exprKind = ExprKind.Kind.CALL;
    this.call = checkNotNull(call);
  }

  public void setCreateList(CelMutableCreateList createList) {
    this.exprKind = ExprKind.Kind.CREATE_LIST;
    this.createList = checkNotNull(createList);
  }

  /** A mutable identifier expression. */
  public static final class CelMutableIdent {
    private String name = "";

    public String name() {
      return name;
    }

    public void setName(String name) {
      this.name = checkNotNull(name);
    }

    public static CelMutableIdent create(String name) {
      return new CelMutableIdent(name);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof CelMutableIdent) {
        CelMutableIdent that = (CelMutableIdent) obj;
        return this.name.equals(that.name);
      }

      return false;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    private CelMutableIdent(String name) {
      this.name = checkNotNull(name);
    }
  }

  /** A mutable field selection expression. e.g. `request.auth`. */
  public static final class CelMutableSelect {
    private CelMutableExpr operand;
    private String field = "";
    private boolean testOnly;

    public CelMutableExpr operand() {
      return operand;
    }

    public void setOperand(CelMutableExpr operand) {
      this.operand = checkNotNull(operand);
    }

    public String field() {
      return field;
    }

    public void setField(String field) {
      this.field = checkNotNull(field);
    }

    public boolean testOnly() {
      return testOnly;
    }

    public void setTestOnly(boolean testOnly) {
      this.testOnly = testOnly;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof CelMutableSelect) {
        CelMutableSelect that = (CelMutableSelect) obj;
        return this.operand.equals(that.operand())
            && this.field.equals(that.field())
            && this.testOnly == that.testOnly();
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= operand.hashCode();
      h *= 1000003;
      h ^= field.hashCode();
      h *= 1000003;
      h ^= testOnly ? 1231 : 1237;
      return h;
    }

    public static CelMutableSelect create(CelMutableExpr operand, String field) {
      return new CelMutableSelect(operand, field, false);
    }

    public static CelMutableSelect create(CelMutableExpr operand, String field, boolean testOnly) {
      return new CelMutableSelect(operand, field, testOnly);
    }

    private CelMutableSelect(CelMutableExpr operand, String field, boolean testOnly) {
      this.operand = checkNotNull(operand);
      this.field = checkNotNull(field);
      this.testOnly = testOnly;
    }
  }

  /** A mutable call expression, including calls to predefined functions and operators. */
  public static final class CelMutableCall {
    private Optional<CelMutableExpr> target;
    private String function;
    private List<CelMutableExpr> args;

    public Optional<CelMutableExpr> target() {
      return target;
    }

    public void setTarget(CelMutableExpr target) {
      this.target = Optional.of(target);
    }

    public String function() {
      return function;
    }

    public void setFunction(String function) {
      this.function = checkNotNull(function);
    }

    public List<CelMutableExpr> args() {
      return args;
    }

    public void clearArgs() {
      args.clear();
    }

    public void addArgs(CelMutableExpr... exprs) {
      addArgs(Arrays.asList(checkNotNull(exprs)));
    }

    public void addArgs(Iterable<CelMutableExpr> exprs) {
      exprs.forEach(e -> args.add(checkNotNull(e)));
    }

    public void setArgs(Collection<CelMutableExpr> exprs) {
      this.args = new ArrayList<>(checkNotNull(exprs));
    }

    public void setArg(int index, CelMutableExpr arg) {
      checkArgument(index >= 0 && index < args.size());
      args.set(index, checkNotNull(arg));
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof CelMutableCall) {
        CelMutableCall that = (CelMutableCall) obj;
        return this.target.equals(that.target())
            && this.function.equals(that.function())
            && this.args.equals(that.args());
      }

      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= target.hashCode();
      h *= 1000003;
      h ^= function.hashCode();
      h *= 1000003;
      h ^= args.hashCode();
      return h;
    }

    public static CelMutableCall create(String function, CelMutableExpr... args) {
      return create(function, Arrays.asList(checkNotNull(args)));
    }

    public static CelMutableCall create(String function, List<CelMutableExpr> args) {
      return new CelMutableCall(function, args);
    }

    public static CelMutableCall create(
        CelMutableExpr target, String function, CelMutableExpr... args) {
      return create(target, function, Arrays.asList(checkNotNull(args)));
    }

    public static CelMutableCall create(
        CelMutableExpr target, String function, List<CelMutableExpr> args) {
      return new CelMutableCall(target, function, args);
    }

    private CelMutableCall(String function, List<CelMutableExpr> args) {
      this.target = Optional.empty();
      this.function = checkNotNull(function);
      this.args = new ArrayList<>(checkNotNull(args));
    }

    private CelMutableCall(CelMutableExpr target, String function, List<CelMutableExpr> args) {
      this(function, args);
      this.target = Optional.of(target);
    }
  }

  /**
   * A mutable list creation expression.
   *
   * <p>Lists may either be homogenous, e.g. `[1, 2, 3]`, or heterogeneous, e.g. `dyn([1, 'hello',
   * 2.0])`
   */
  public static final class CelMutableCreateList {
    private final List<CelMutableExpr> elements;
    private final List<Integer> optionalIndices;

    public List<CelMutableExpr> elements() {
      return elements;
    }

    public void setElement(int index, CelMutableExpr element) {
      checkArgument(index >= 0 && index < elements().size());
      elements.set(index, checkNotNull(element));
    }

    public List<Integer> optionalIndices() {
      return optionalIndices;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof CelMutableCreateList) {
        CelMutableCreateList that = (CelMutableCreateList) obj;
        return this.elements.equals(that.elements())
            && this.optionalIndices.equals(that.optionalIndices());
      }

      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= elements.hashCode();
      h *= 1000003;
      h ^= optionalIndices.hashCode();

      return h;
    }

    public static CelMutableCreateList create(CelMutableExpr... elements) {
      return create(Arrays.asList(checkNotNull(elements)));
    }

    public static CelMutableCreateList create(List<CelMutableExpr> elements) {
      return create(elements, new ArrayList<>());
    }

    public static CelMutableCreateList create(
        List<CelMutableExpr> mutableExprList, List<Integer> optionalIndices) {
      return new CelMutableCreateList(mutableExprList, optionalIndices);
    }

    private CelMutableCreateList(
        List<CelMutableExpr> mutableExprList, List<Integer> optionalIndices) {
      this.elements = new ArrayList<>(checkNotNull(mutableExprList));
      this.optionalIndices = new ArrayList<>(checkNotNull(optionalIndices));
    }
  }

  public static CelMutableExpr ofNotSet() {
    return ofNotSet(0L);
  }

  public static CelMutableExpr ofNotSet(long id) {
    return new CelMutableExpr(id);
  }

  public static CelMutableExpr ofConstant(CelConstant constant) {
    return ofConstant(0L, constant);
  }

  public static CelMutableExpr ofConstant(long id, CelConstant constant) {
    return new CelMutableExpr(id, constant);
  }

  public static CelMutableExpr ofIdent(String name) {
    return ofIdent(0, name);
  }

  public static CelMutableExpr ofIdent(long id, String name) {
    return new CelMutableExpr(id, CelMutableIdent.create(name));
  }

  public static CelMutableExpr ofSelect(CelMutableSelect mutableSelect) {
    return ofSelect(0, mutableSelect);
  }

  public static CelMutableExpr ofSelect(long id, CelMutableSelect mutableSelect) {
    return new CelMutableExpr(id, mutableSelect);
  }

  public static CelMutableExpr ofCall(CelMutableCall mutableCall) {
    return ofCall(0, mutableCall);
  }

  public static CelMutableExpr ofCall(long id, CelMutableCall mutableCall) {
    return new CelMutableExpr(id, mutableCall);
  }

  public static CelMutableExpr ofCreateList(CelMutableCreateList mutableCreateList) {
    return ofCreateList(0, mutableCreateList);
  }

  public static CelMutableExpr ofCreateList(long id, CelMutableCreateList mutableCreateList) {
    return new CelMutableExpr(id, mutableCreateList);
  }

  private CelMutableExpr(long id, CelConstant mutableConstant) {
    this.id = id;
    setConstant(mutableConstant);
  }

  private CelMutableExpr(long id, CelMutableIdent mutableIdent) {
    this.id = id;
    setIdent(mutableIdent);
  }

  private CelMutableExpr(long id, CelMutableSelect mutableSelect) {
    this.id = id;
    setSelect(mutableSelect);
  }

  private CelMutableExpr(long id, CelMutableCall mutableCall) {
    this.id = id;
    setCall(mutableCall);
  }

  private CelMutableExpr(long id, CelMutableCreateList mutableCreateList) {
    this.id = id;
    setCreateList(mutableCreateList);
  }

  private CelMutableExpr(long id) {
    this();
    this.id = id;
  }

  private CelMutableExpr() {
    this.notSet = CelExpr.newBuilder().build().exprKind().notSet();
    this.exprKind = ExprKind.Kind.NOT_SET;
  }

  private Object exprValue() {
    switch (this.exprKind) {
      case NOT_SET:
        return notSet();
      case CONSTANT:
        return constant();
      case IDENT:
        return ident();
      case SELECT:
        return select();
      case CALL:
        return call();
      case CREATE_LIST:
        return createList();
      case CREATE_STRUCT:
      case CREATE_MAP:
      case COMPREHENSION:
        // fall-through (not implemented yet)
    }

    throw new IllegalStateException("Unexpected expr kind: " + this.exprKind);
  }

  private void checkExprKind(ExprKind.Kind exprKind) {
    checkArgument(this.exprKind.equals(exprKind), "Invalid ExprKind: %s", exprKind);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof CelMutableExpr) {
      CelMutableExpr that = (CelMutableExpr) obj;
      if (this.id != that.id() || !this.exprKind.equals(that.getKind())) {
        return false;
      }
      // When both objects' hashes are cached and they do not match, they can never be equal.
      if (this.hash != 0 && that.hash != 0 && this.hash != that.hash) {
        return false;
      }
      return this.exprValue().equals(that.exprValue());
    }

    return false;
  }

  @Override
  public int hashCode() {
    if (hash == 0) {
      int h = 1;
      h *= 1000003;
      h ^= (int) ((id >>> 32) ^ id);
      h *= 1000003;
      h ^= this.exprValue().hashCode();

      if (h == 0) {
        h = 1;
      }
      hash = h;
    }

    return hash;
  }
}
