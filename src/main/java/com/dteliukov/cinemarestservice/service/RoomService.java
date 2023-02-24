package com.dteliukov.cinemarestservice.service;

import com.dteliukov.cinemarestservice.exception.AlreadyPurchasedTicketException;
import com.dteliukov.cinemarestservice.exception.SeatOutOfBoundsException;
import com.dteliukov.cinemarestservice.model.common.PurchasedTicket;
import com.dteliukov.cinemarestservice.model.common.Room;
import com.dteliukov.cinemarestservice.model.common.Seat;
import com.dteliukov.cinemarestservice.model.common.Ticket;
import com.dteliukov.cinemarestservice.model.property.PriceRangeProperty;
import com.dteliukov.cinemarestservice.model.property.SeatRangeProperty;
import com.dteliukov.cinemarestservice.model.response.RoomInfo;
import com.dteliukov.cinemarestservice.repository.RoomRepository;
import com.dteliukov.cinemarestservice.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final TicketRepository ticketRepository;
    private final SeatRangeProperty rowSeatRange;
    private final SeatRangeProperty columnSeatRange;

    private final PriceRangeProperty priceRangeProperty;
    @Autowired
    public RoomService(RoomRepository roomRepository,
                       TicketRepository ticketRepository,
                       @Qualifier("column_seat_range") SeatRangeProperty rowSeatRange,
                       @Qualifier("row_seat_range") SeatRangeProperty columnSeatRange,
                       PriceRangeProperty priceRangeProperty) {
        this.roomRepository = roomRepository;
        this.ticketRepository = ticketRepository;
        this.rowSeatRange = rowSeatRange;
        this.columnSeatRange = columnSeatRange;
        this.priceRangeProperty = priceRangeProperty;
    }


    public RoomInfo getRoomInfo() {
        Room room = roomRepository.getRoom();
        return new RoomInfo(room.getTotalRows(), room.getTotalColumns(), room.getSeats().stream().toList());
    }

    public PurchasedTicket purchase(Seat seat) {
        validateSeat(seat);
        roomRepository.deleteSeat(seat);
        Ticket ticket = generateTicket(seat);
        return ticketRepository.save(ticket);
    }

    private Ticket generateTicket(Seat seat) {
        int price = calcPrice(seat);
        return new Ticket(seat.row(), seat.column(), price);
    }

    private int calcPrice(Seat seat) {
        return (seat.row() <= priceRangeProperty.borderRow()) ?
                priceRangeProperty.firstPrice() : priceRangeProperty.secondPrice();
    }

    private void validateSeat(Seat seat) {
        if (checkSeat(seat)) {
            throw new SeatOutOfBoundsException();
        } else if (!roomRepository.isExistSeat(seat)) {
            throw new AlreadyPurchasedTicketException();
        }
    }

    private boolean checkSeat(Seat seat) {
        return !(checkSeatRange(seat.row(), rowSeatRange.min(), rowSeatRange.max()) &&
                checkSeatRange(seat.column(), columnSeatRange.min(), columnSeatRange.max()));
    }

    private boolean checkSeatRange(int seatNumber, int min, int max) {
        return min < seatNumber && seatNumber <= max;
    }
}
