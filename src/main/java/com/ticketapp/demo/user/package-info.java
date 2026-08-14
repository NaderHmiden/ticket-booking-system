/**
 * User bounded context — minimal by design. Registration/auth via standard
 * Spring Security + JWT patterns, just enough to attribute bookings to a
 * user. Not where the engineering value of this project lives (see
 * README.md scope section).
 *
 * <p>Only classes in this package (not {@code .internal}) are the public
 * surface other contexts may depend on.
 */
package com.ticketapp.demo.user;
