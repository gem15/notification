package com.severtrans.notification;

import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static void emailAlert(String alert) {
        System.out.println("+++++++++++++"+alert);
        
    }

    /**
     * example
     * List<UserDTO> userDtoList = mapList(users, UserDTO.class);
     * @param source
     * @param targetClass
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S, T> List<T> mapList(List<S> source, Class<T> targetClass,ModelMapper modelMapper) {
        return source
                .stream()
                .map(element -> modelMapper.map(element, targetClass))
                .collect(Collectors.toList());
    }

}
