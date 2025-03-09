package com.lucalabs.naturescompass.utils;

public enum CompassState {

	INACTIVE(0), SEARCHING(1), FOUND(2), NOT_FOUND(3);

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