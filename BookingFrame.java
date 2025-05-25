package com.movie.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import javax.swing.*;
import com.movie.bus.MovieBUS;
import com.movie.bus.RoomBUS;
import com.movie.bus.ShowtimeBUS;
import com.movie.bus.TicketBUS;
import com.movie.bus.SeatBUS;
import com.movie.model.Movie;
import com.movie.model.Room;
import com.movie.model.Seat;
import com.movie.model.Showtime;
import com.movie.network.SocketClient;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial") // Bỏ qua cảnh báo serialization
public class BookingFrame extends JFrame {
    private final SocketClient client; // Đánh dấu final
    private JPanel seatPanel;
    private JLabel totalCostLabel;
    private JLabel movieInfoLabel;
    private final List<JButton> seatButtons; // Đánh dấu final
    private final List<String> selectedSeats; // Đánh dấu final
    private int ticketPrice; // Giá vé động, lấy từ Room
    private final int customerId; // Đánh dấu final
    private final int showtimeId; // Đánh dấu final
    private final int roomId; // Đánh dấu final
    private final int movieId; // Đánh dấu final
    private final MovieBUS movieBUS;
    private final RoomBUS roomBUS;
    private final ShowtimeBUS showtimeBUS;
    private final TicketBUS ticketBUS;
    private final SeatBUS seatBUS;
    private List<Seat> allSeats;
    private Map<String, Integer> seatNameToIdMap;

    public BookingFrame(int customerId, int roomId, int movieId, int showtimeId) {
        this.customerId = customerId;
        this.roomId = roomId;
        this.movieId = movieId;
        this.showtimeId = showtimeId; // Nhận showtimeId từ UserFrame
        this.seatButtons = new ArrayList<>();
        this.selectedSeats = new ArrayList<>();
        this.movieBUS = new MovieBUS();
        this.roomBUS = new RoomBUS();
        this.showtimeBUS = new ShowtimeBUS();
        this.ticketBUS = new TicketBUS();
        this.seatBUS = new SeatBUS();
        this.seatNameToIdMap = new HashMap<>();
        this.client = new SocketClient("localhost", 5000);
        initUI();
        initSocket();
        loadShowtime();
        loadSeats();
    }

    private void initUI() {
        setTitle("Đặt vé xem phim");
        setSize(600, 400);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.DARK_GRAY);

        JPanel infoPanel = new JPanel(new FlowLayout());
        infoPanel.setBackground(Color.LIGHT_GRAY);
        movieInfoLabel = new JLabel("Đang tải thông tin...");
        infoPanel.add(movieInfoLabel);
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        seatPanel = new JPanel();
        seatPanel.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(seatPanel);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel costPanel = new JPanel(new FlowLayout());
        costPanel.setBackground(Color.LIGHT_GRAY);
        totalCostLabel = new JLabel("Tổng chi phí: 0 VND");
        costPanel.add(totalCostLabel);
        JButton bookButton = new JButton("Đặt vé");
        bookButton.addActionListener(e -> bookTickets());
        costPanel.add(bookButton);
        mainPanel.add(costPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void loadShowtime() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                try {
                    Movie movie = movieBUS.getMovieById(movieId);
                    Room room = roomBUS.getRoomById(roomId);
                    List<Showtime> showtimes = showtimeBUS.getAllShowtimes();
                    Showtime showtime = showtimes.stream()
                            .filter(s -> s.getShowtimeID() == showtimeId && s.getIsVisible() == 0)
                            .findFirst()
                            .orElse(null);

                    if (movie != null && room != null && showtime != null) {
                        ticketPrice = (int) room.getPrice(); // Lấy giá vé từ Room
                        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");
                        String showDateStr = dateFormat.format(showtime.getShowDate());

                        SwingUtilities.invokeLater(() -> {
                            movieInfoLabel.setText("Phim: " + movie.getTitle() +
                                    " | Rạp: CGV Vincom | Phòng: " + room.getRoomName() +
                                    " | Suất: " + showDateStr);
                        });

                        if (client != null && client.isConnected()) {
                            client.sendMessage("GET_SEATS:" + showtimeId + ":" + roomId);
                        }
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(BookingFrame.this,
                                    "Không tìm thấy thông tin suất chiếu hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                            dispose();
                        });
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(BookingFrame.this,
                            "Lỗi tải thông tin suất chiếu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }
        }.execute();
    }

    private void loadSeats() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    Movie movie = movieBUS.getMovieById(movieId);
                    Room room = roomBUS.getRoomById(roomId);
                    List<Showtime> showtimes = showtimeBUS.getShowtimesByRoomAndMovie(roomId, movieId);
                    allSeats = seatBUS.getSeatsByRoomId(roomId);

                    if (movie != null && room != null && !showtimes.isEmpty() && !allSeats.isEmpty()) {
                        Showtime showtime = showtimes.get(0);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");
                        String showDateStr = dateFormat.format(showtime.getShowDate());

                        seatNameToIdMap.clear();
                        for (Seat seat : allSeats) {
                            seatNameToIdMap.put(seat.getSeatNumber(), seat.getSeatID());
                        }

                        SwingUtilities.invokeLater(() -> {
                            movieInfoLabel.setText("Phim: " + movie.getTitle() +
                                    " | Rạp: CGV Vincom | Phòng: " + room.getRoomName() +
                                    " | Suất: " + showDateStr);
                            seatPanel.removeAll();
                            seatPanel.setLayout(new GridLayout(0, 5, 10, 10));
                            seatButtons.clear();
                            for (Seat seat : allSeats) {
                                JButton seatButton = new JButton(seat.getSeatNumber());
                                seatButton.setBackground(Color.GREEN);
                                seatButton.addActionListener(e -> toggleSeat(seatButton));
                                seatButtons.add(seatButton);
                                seatPanel.add(seatButton);
                                checkSeatStatus(seatButton, seat.getSeatNumber());
                            }
                            seatPanel.revalidate();
                            seatPanel.repaint();
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(BookingFrame.this,
                                    "Không tải được thông tin phim, phòng hoặc ghế cho RoomID " + roomId + "!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                            dispose();
                        });
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(BookingFrame.this,
                            "Lỗi tải ghế: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }
        }.execute();
    }

    private void checkSeatStatus(JButton seatButton, String seatName) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    Integer seatId = seatNameToIdMap.get(seatName);
                    if (seatId == null) return false;
                    return ticketBUS.isSeatBooked(seatId, showtimeId);
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        seatButton.setBackground(Color.RED);
                        seatButton.setEnabled(false);
                        selectedSeats.remove(seatName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private int getSeatIdFromName(String seatName) {
        return seatNameToIdMap.getOrDefault(seatName, -1);
    }

    private void toggleSeat(JButton seatButton) {
        String seatName = seatButton.getText();
        if (!seatButton.isEnabled()) return;
        if (seatButton.getBackground().equals(Color.GREEN)) {
            seatButton.setBackground(Color.YELLOW);
            selectedSeats.add(seatName);
        } else if (seatButton.getBackground().equals(Color.YELLOW)) {
            seatButton.setBackground(Color.GREEN);
            selectedSeats.remove(seatName);
        }
        updateTotalCost();
    }

    private void updateTotalCost() {
        int totalCost = selectedSeats.size() * ticketPrice;
        totalCostLabel.setText("Tổng chi phí: " + totalCost + " VND");
    }

    private void initSocket() {
        client.start();
        try {
            client.waitForConnection(); // Wait for connection to be established
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!client.isConnected()) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        client.addMessageListener(new SocketClient.MessageListener() {
            @Override
            public void onMessage(String message) {
                SwingUtilities.invokeLater(() -> {
                    if (message.startsWith("SEAT_UPDATE")) {
                        updateSeats(message.replace("SEAT_UPDATE", "").trim());
                    }
                });
            }
        });
    }

    private void updateSeats(String seatData) {
        if (seatData.isEmpty()) return;
        String[] lockedSeats = seatData.split(",");
        for (JButton seatButton : seatButtons) {
            String seatName = seatButton.getText();
            boolean isLocked = false;
            for (String lockedSeat : lockedSeats) {
                if (seatName.equals(lockedSeat.trim())) {
                    isLocked = true;
                    break;
                }
            }
            if (isLocked && seatButton.isEnabled()) {
                seatButton.setBackground(Color.RED);
                seatButton.setEnabled(false);
                selectedSeats.remove(seatName);
            } else if (!selectedSeats.contains(seatName)) {
                seatButton.setBackground(Color.GREEN);
                seatButton.setEnabled(true);
            }
        }
        updateTotalCost();
    }

    private void bookTickets() {
        if (selectedSeats.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất một ghế!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (String seat : selectedSeats) {
            try {
                int seatId = getSeatIdFromName(seat);
                if (seatId == -1) {
                    JOptionPane.showMessageDialog(this, "Ghế " + seat + " không hợp lệ trong RoomID " + roomId + "!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (ticketBUS.isSeatBooked(seatId, showtimeId)) {
                    JOptionPane.showMessageDialog(this, "Ghế " + seat + " đã được đặt!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi kiểm tra ghế: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String message = "LOCK_SEATS:" + showtimeId + ":" + roomId + ":" + String.join(",", selectedSeats);
        if (client != null && client.isConnected()) {
            client.sendMessage(message);
            SwingUtilities.invokeLater(() -> {
                new PaymentFrame(selectedSeats, selectedSeats.size() * ticketPrice, showtimeId, roomId, customerId, movieId, roomId, seatNameToIdMap).setVisible(true);
                dispose();
            });
        } else {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}