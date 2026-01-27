package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect to measure and log database query execution time
 * Automatically tracks latency for all repository method calls
 */
@Aspect
@Component
@Slf4j
//SLF4J (Simple Logging Facade for Java) is a popular Java logging abstraction that acts as a standard interface, decoupling your application code from specific logging frameworks like Logback
public class RepositoryPerformanceAspect {

    // Threshold for slow query warning (in milliseconds)
    private static final long SLOW_QUERY_THRESHOLD_MS = 100;

    /**
     * Intercept all repository method calls and measure execution time
     * 
     * Pointcut explanation:where exactly u want to call that method or aspect ..
     * - execution(* org.example.repository..*(..)) : All methods in repository package
     * - First * : Any return type
     * - org.example.repository..* : Any class in repository package and sub-packages
     * - *(..) : Any method with any parameters
     */

    @Around("execution(* org.example.repository..*(..))")
    public Object measureRepositoryLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get method details
        String className = joinPoint.getTarget().getClass().getSimpleName();
        // what are join point? -> these are basically candidates on or befire u want to put that logging , like getter setter ke pehle ya constructor call ke pehle,, ya before annotation..
        //The whole code written in calleed "Advice"
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // Start timing
        long startTime = System.currentTimeMillis();
        
        Object result = null;
        boolean success = true;
        
        try {
            // Execute the actual repository method
            result = joinPoint.proceed();
            return result;
            
        } catch (Throwable throwable) {
            success = false;
            throw throwable;
            
        } finally {
            // Calculate execution time
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Log based on execution time and success
            logQueryPerformance(className, methodName, args, executionTime, success, result);
        }
    }

    /**
     * Log query performance with appropriate log level based on execution time
     */
    private void logQueryPerformance(String className, String methodName, Object[] args, 
                                     long executionTime, boolean success, Object result) {
        
        String logMessage = buildLogMessage(className, methodName, args, executionTime, result);
        
        if (!success) {
            // Failed queries - Always log as ERROR
            log.error("❌ DB Query FAILED | {}", logMessage);
            
        } else if (executionTime >= SLOW_QUERY_THRESHOLD_MS) {
            // Slow queries - Log as WARNING
            log.warn("🐌 SLOW DB Query | {}", logMessage);
            
        } else {
            // Normal queries - Log as DEBUG (only in development)
            log.debug("✅ DB Query OK | {}", logMessage);
        }
        
        // Additional INFO log for all queries with metrics (optional - for monitoring)
        if (log.isInfoEnabled()) {
            log.info("📊 DB Metrics | Repository={} | Method={} | Latency={}ms | Success={}", 
                    className, methodName, executionTime, success);
        }
    }

    /**
     * Build formatted log message with all relevant details
     */
    private String buildLogMessage(String className, String methodName, Object[] args, 
                                   long executionTime, Object result) {
        StringBuilder message = new StringBuilder();
        
        message.append("Repository=").append(className)
               .append(" | Method=").append(methodName)
               .append(" | Latency=").append(executionTime).append("ms");
        
        // Add parameter info (first 3 params only to avoid huge logs)
        if (args != null && args.length > 0) {
            message.append(" | Params=[");
            int paramCount = Math.min(args.length, 3);
            for (int i = 0; i < paramCount; i++) {
                if (i > 0) message.append(", ");
                message.append(formatArgument(args[i]));
            }
            if (args.length > 3) {
                message.append(", ... (").append(args.length - 3).append(" more)");
            }
            message.append("]");
        }
        
        // Add result info
        if (result != null) {
            if (result instanceof java.util.Collection) {
                message.append(" | ResultCount=").append(((java.util.Collection<?>) result).size());
            } else if (result instanceof java.util.Optional) {
                message.append(" | ResultPresent=").append(((java.util.Optional<?>) result).isPresent());
            } else {
                message.append(" | ResultType=").append(result.getClass().getSimpleName());
            }
        } else {
            message.append(" | Result=null");
        }
        
        return message.toString();
    }

    /**
     * Format argument for logging (handle sensitive data)
     */
    private String formatArgument(Object arg) {
        if (arg == null) {
            return "null";
        }
        
        String argString = arg.toString();
        
        // Truncate long strings
        if (argString.length() > 50) {
            return argString.substring(0, 50) + "...";
        }
        
        return argString;
    }
}
