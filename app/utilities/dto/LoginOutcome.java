package utilities.dto;

import play.mvc.Call;

public record LoginOutcome(
        LoginResult loginResult,
        Call redirectPath
) {}

