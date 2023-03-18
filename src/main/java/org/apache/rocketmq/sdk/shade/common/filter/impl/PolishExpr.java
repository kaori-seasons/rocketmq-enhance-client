package org.apache.rocketmq.sdk.shade.common.filter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class PolishExpr {
    public static List<Op> reversePolish(String expression) {
        return reversePolish(participle(expression));
    }

    public static List<Op> reversePolish(List<Op> tokens) {
        ArrayList arrayList = new ArrayList();
        Stack<Operator> operatorStack = new Stack<>();
        for (int i = 0; i < tokens.size(); i++) {
            Op token = tokens.get(i);
            if (isOperand(token)) {
                arrayList.add(token);
            } else if (isLeftParenthesis(token)) {
                operatorStack.push((Operator) token);
            } else if (isRightParenthesis(token)) {
                Operator opNew = null;
                while (!operatorStack.empty()) {
                    Operator operator = Operator.LEFTPARENTHESIS;
                    Operator pop = operatorStack.pop();
                    opNew = pop;
                    if (operator == pop) {
                        break;
                    }
                    arrayList.add(opNew);
                }
                if (null == opNew || Operator.LEFTPARENTHESIS != opNew) {
                    throw new IllegalArgumentException("mismatched parentheses");
                }
            } else if (isOperator(token)) {
                Operator opNew2 = (Operator) token;
                if (!operatorStack.empty()) {
                    Operator opOld = operatorStack.peek();
                    if (opOld.isCompareable() && opNew2.compare(opOld) != 1) {
                        arrayList.add(operatorStack.pop());
                    }
                }
                operatorStack.push(opNew2);
            } else {
                throw new IllegalArgumentException("illegal token " + token);
            }
        }
        while (!operatorStack.empty()) {
            Operator operator2 = operatorStack.pop();
            if (Operator.LEFTPARENTHESIS == operator2 || Operator.RIGHTPARENTHESIS == operator2) {
                throw new IllegalArgumentException("mismatched parentheses " + operator2);
            }
            arrayList.add(operator2);
        }
        return arrayList;
    }

    private static List<Op> participle(String expression) {
        List<Op> segments = new ArrayList<>();
        int size = expression.length();
        int wordStartIndex = -1;
        int wordLen = 0;
        Type preType = Type.NULL;
        for (int i = 0; i < size; i++) {
            int chValue = expression.charAt(i);
            if ((97 <= chValue && chValue <= 122) || ((65 <= chValue && chValue <= 90) || ((49 <= chValue && chValue <= 57) || 95 == chValue))) {
                if (Type.OPERATOR == preType || Type.SEPAERATOR == preType || Type.NULL == preType || Type.PARENTHESIS == preType) {
                    if (Type.OPERATOR == preType) {
                        segments.add(Operator.createOperator(expression.substring(wordStartIndex, wordStartIndex + wordLen)));
                    }
                    wordStartIndex = i;
                    wordLen = 0;
                }
                preType = Type.OPERAND;
                wordLen++;
            } else if (40 == chValue || 41 == chValue) {
                if (Type.OPERATOR == preType) {
                    segments.add(Operator.createOperator(expression.substring(wordStartIndex, wordStartIndex + wordLen)));
                    wordStartIndex = -1;
                    wordLen = 0;
                } else if (Type.OPERAND == preType) {
                    segments.add(new Operand(expression.substring(wordStartIndex, wordStartIndex + wordLen)));
                    wordStartIndex = -1;
                    wordLen = 0;
                }
                preType = Type.PARENTHESIS;
                segments.add(Operator.createOperator(((char) chValue) + ""));
            } else if (38 == chValue || 124 == chValue) {
                if (Type.OPERAND == preType || Type.SEPAERATOR == preType || Type.PARENTHESIS == preType) {
                    if (Type.OPERAND == preType) {
                        segments.add(new Operand(expression.substring(wordStartIndex, wordStartIndex + wordLen)));
                    }
                    wordStartIndex = i;
                    wordLen = 0;
                }
                preType = Type.OPERATOR;
                wordLen++;
            } else if (32 == chValue || 9 == chValue) {
                if (Type.OPERATOR == preType) {
                    segments.add(Operator.createOperator(expression.substring(wordStartIndex, wordStartIndex + wordLen)));
                    wordStartIndex = -1;
                    wordLen = 0;
                } else if (Type.OPERAND == preType) {
                    segments.add(new Operand(expression.substring(wordStartIndex, wordStartIndex + wordLen)));
                    wordStartIndex = -1;
                    wordLen = 0;
                }
                preType = Type.SEPAERATOR;
            } else {
                throw new IllegalArgumentException("illegal expression, at index " + i + " " + ((char) chValue));
            }
        }
        if (wordLen > 0) {
            segments.add(new Operand(expression.substring(wordStartIndex, wordStartIndex + wordLen)));
        }
        return segments;
    }

    public static boolean isOperand(Op token) {
        return token instanceof Operand;
    }

    public static boolean isLeftParenthesis(Op token) {
        return (token instanceof Operator) && Operator.LEFTPARENTHESIS == ((Operator) token);
    }

    public static boolean isRightParenthesis(Op token) {
        return (token instanceof Operator) && Operator.RIGHTPARENTHESIS == ((Operator) token);
    }

    public static boolean isOperator(Op token) {
        return token instanceof Operator;
    }
}
