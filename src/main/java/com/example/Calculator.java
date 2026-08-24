package com.example;

public class Calculator {

    public double add(double a, double b) {
        return a + b;
    }

    public double subtract(double a, double b) {
        return a - b;
    }

    public double multiply(double a, double b) {
        return a * b;
    }

    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return a / b;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: Calculator <number1> <operator> <number2>");
            System.out.println("Operators: + - * /");
            return;
        }

        double a = Double.parseDouble(args[0]);
        String operator = args[1];
        double b = Double.parseDouble(args[2]);

        Calculator calculator = new Calculator();
        double result;

        switch (operator) {
            case "+":
                result = calculator.add(a, b);
                break;
            case "-":
                result = calculator.subtract(a, b);
                break;
            case "*":
                result = calculator.multiply(a, b);
                break;
            case "/":
                result = calculator.divide(a, b);
                break;
            default:
                System.out.println("Unknown operator: " + operator);
                return;
        }

        System.out.println(result);
    }
}
