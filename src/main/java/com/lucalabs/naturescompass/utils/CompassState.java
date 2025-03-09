package com.lucalabs.naturescompass.utils;

public enum CompassState {

	INACTIVE(0), SEARCHING(1), FOUND_CLOSEST(2), FOUND_SECOND_CLOSEST(3), SECOND_CLOSEST_NOT_FOUND(4), CLOSEST_NOT_FOUND(5);

	private int id;

	CompassState(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static CompassState fromId(int id) {
		for (CompassState state : values()) {
			if (state.getId() == id) {
				return state;
			}
		}

		return null;
	}

}