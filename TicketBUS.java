package com.movie.bus;

import com.movie.dao.BookingHistoryDAO;
import com.movie.dao.TicketDAO;
import com.movie.model.BookingHistory;
import com.movie.model.Seat;
import com.movie.model.Ticket;
import com.movie.network.SocketClient;
import com.movie.network.ThreadManager;

import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class TicketBUS {
    private final TicketDAO ticketDAO = new TicketDAO();
    private final BookingHistoryDAO bookingHistoryDAO = new BookingHistoryDAO();

    public String processPayment(int customerID, int showtimeID, List<Seat> seats, double totalPrice, String movieTitle, String roomName) throws SQLException {
        if (customerID <= 0 || showtimeID <= 0 || seats == null || seats.isEmpty() || totalPrice < 0) {
            JOptionPane.showMessageDialog(null,
                    "Thông tin đặt vé không hợp lệ",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return "Thông tin đặt vé không hợp lệ";
        }

        for (Seat seat : seats) {
            if (ticketDAO.isSeatBooked(seat.getSeatID(), showtimeID)) {
                String message = "Ghế " + seat.getSeatNumber() + " đã được đặt!";
                JOptionPane.showMessageDialog(null,
                        message,
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return message;
            }
        }

        Connection conn = null;
        try {
            conn = com.movie.util.DBConnection.getConnection();
            conn.setAutoCommit(false);

            for (Seat seat : seats) {
                Ticket ticket = new Ticket();
                ticket.setCustomerID(customerID);
                ticket.setShowtimeID(showtimeID);
                ticket.setSeatID(seat.getSeatID());
                ticket.setPrice((int) (totalPrice / seats.size()));
                ticket.setSeatNumber(seat.getSeatNumber());
                int ticketID = ticketDAO.bookTicket(ticket);

                BookingHistory history = new BookingHistory();
                history.setCustomerID(customerID);
                history.setTicketID(ticketID);
                history.setBookingDate(new Date());
                history.setMovieTitle(movieTitle);
                history.setRoomName(roomName);
                history.setSeatNumber(seat.getSeatNumber());
                history.setPrice((int) (totalPrice / seats.size()));
                bookingHistoryDAO.addBooking(history);
            }

            conn.commit();

            ThreadManager.execute(() -> {
                SocketClient client = new SocketClient("localhost", 5000);
                client.start();
                try {
                    client.waitForConnection();
                    if (client.isConnected()) {
                        client.sendMessage("SEAT_UPDATE:" + showtimeID + ":" + seats.get(0).getRoomID() + ":" + getSeatNumbers(seats));
                        // Add a small delay to ensure the server processes the message
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while waiting for socket connection: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupted status
                } catch (Exception e) {
                    System.err.println("Error in socket communication: " + e.getMessage());
                } finally {
                    client.stop();
                }
            });

            JOptionPane.showMessageDialog(null,
                    "Thanh toán thành công!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            return "Thanh toán thành công!";
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            System.err.println("Error processing payment for customer " + customerID + ": " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Không thể xử lý thanh toán: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("Error closing connection: " + closeEx.getMessage());
                }
            }
        }
    }

    public List<BookingHistory> getBookingHistory(int customerID) throws SQLException {
        if (customerID <= 0) {
            JOptionPane.showMessageDialog(null,
                    "ID khách hàng không hợp lệ",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            throw new IllegalArgumentException("ID khách hàng không hợp lệ");
        }

        try {
            return bookingHistoryDAO.getBookingsByCustomer(customerID);
        } catch (SQLException e) {
            System.err.println("Error retrieving booking history for customer " + customerID + ": " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Không thể tải lịch sử đặt vé: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    public boolean isSeatBooked(int seatID, int showtimeID) throws SQLException {
        return ticketDAO.isSeatBooked(seatID, showtimeID);
    }

    private String getSeatNumbers(List<Seat> seats) {
        StringBuilder seatNumbers = new StringBuilder();
        for (int i = 0; i < seats.size(); i++) {
            seatNumbers.append(seats.get(i).getSeatNumber());
            if (i < seats.size() - 1) {
                seatNumbers.append(",");
            }
        }
        return seatNumbers.toString();
    }
}
