package com.example.template.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP Aspect untuk logging otomatis semua method di controller layer.
 * Log entry (saat masuk), exit (saat keluar), dan durasi eksekusi.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // Pointcut: semua method di semua class dalam package controller
    @Pointcut("execution(* com.example.template.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * Around advice: berjalan sebelum dan sesudah method dipanggil.
     * Mencatat nama class, method, argument, dan durasi eksekusi.
     */
    @Around("controllerMethods()")
    public Object logAroundControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("[ENTRY] {}.{}() | args: {}", className, methodName, Arrays.toString(args));

        long startTime = System.currentTimeMillis();
        Object result;

        try {
            result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[EXIT] {}.{}() | duration: {}ms", className, methodName, duration);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[ERROR] {}.{}() | duration: {}ms | error: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }

        return result;
    }
}
