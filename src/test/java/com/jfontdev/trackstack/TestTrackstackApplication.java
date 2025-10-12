package com.jfontdev.trackstack;

import org.springframework.boot.SpringApplication;

public class TestTrackstackApplication {

	public static void main(String[] args) {
		SpringApplication.from(TrackstackApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
