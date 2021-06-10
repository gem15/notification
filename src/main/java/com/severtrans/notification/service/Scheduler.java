package com.severtrans.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
public class Scheduler {
//    @Scheduled(fixedDelay = 10000, initialDelay = 3000)
    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void fixedDelaySch() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
//        System.out.println("Fixed Delay scheduler:: " + strDate);
        log.info("log "+strDate);
    }
}
