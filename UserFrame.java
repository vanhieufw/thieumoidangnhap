package com.movie.ui;

import com.movie.bus.MovieBUS;
import com.movie.bus.RoomBUS;
import com.movie.bus.ShowtimeBUS;
import com.movie.bus.TicketBUS;
import com.movie.model.BookingHistory;
import com.movie.model.Movie;
import com.movie.model.Room;
import com.movie.model.Showtime;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class UserFrame extends JFrame {
    private final int customerID;
    private final MovieBUS movieBUS;
    private final RoomBUS roomBUS;
    private final TicketBUS ticketBUS;
    private final ShowtimeBUS showtimeBUS;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JLabel timeLabel;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private JPanel movieListPanel;
    private JPanel historyListPanel;
    private Thread clockThread;

    public UserFrame(int customerID) {
        this.customerID = customerID;
        this.movieBUS = new MovieBUS();
        this.roomBUS = new RoomBUS();
        this.ticketBUS = new TicketBUS();
        this.showtimeBUS = new ShowtimeBUS();
        userUI();
        SwingUtilities.invokeLater(this::initUI);
        startClock();
    }
    private void userUI() {
        // Tùy chỉnh giao diện cho các nút trong JOptionPane
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.background", new Color(75, 192, 192)); // Xanh lam cho nút OK, Yes, No
        UIManager.put("Button.border", BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Loại bỏ viền mặc định, thay bằng viền mỏng
        UIManager.put("Button.focus", false); // Loại bỏ viền focus
        UIManager.put("Button.margin", new Insets(0, 0, 0, 0)); // Loại bỏ padding thừa
        UIManager.put("Button.arc", 10); // Thêm viền bo góc (nếu hỗ trợ, tùy thuộc theme)

        // Tùy chỉnh font và màu cho các thành phần khác của JOptionPane
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("OptionPane.background", new Color(245, 245, 245));
        UIManager.put("Panel.background", new Color(245, 245, 245));

        // Tùy chỉnh font mặc định cho các thành phần khác
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
    }
    private void initUI() {
        setTitle("Hệ thống bán vé xem phim - Người dùng");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running.set(false);
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel sidebar = createSidebar();
        mainPanel.add(sidebar, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(createMoviesPanel(), "Movies");
        contentPanel.add(createHistoryPanel(), "History");

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(Color.BLACK);
        timePanel.add(timeLabel);
        mainPanel.add(timePanel, BorderLayout.NORTH);

        getContentPane().add(mainPanel);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(40, 40, 40));

        JButton moviesButton = new JButton("Danh sách phim");
        JButton historyButton = new JButton("Lịch sử đặt vé");
        JButton logoutButton = new JButton("Đăng xuất");

        styleButton(moviesButton);
        styleButton(historyButton);
        styleButton(logoutButton);

        moviesButton.addActionListener(e -> showPanel("Movies"));
        historyButton.addActionListener(e -> showPanel("History"));
        logoutButton.addActionListener(e -> logout());

        sidebar.add(Box.createVerticalStrut(30));
        sidebar.add(moviesButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(historyButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(logoutButton);
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private void logout() {
        running.set(false);
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private void startClock() {
        clockThread = new Thread(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            while (running.get()) {
                try {
                    if (timeLabel != null) {
                        SwingUtilities.invokeLater(() ->
                                timeLabel.setText(sdf.format(new java.util.Date())));
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        clockThread.setDaemon(true);
        clockThread.start();
    }

    private void styleButton(JButton button) {
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        button.setBackground(new Color(60, 60, 60));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(new Color(80, 80, 80));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(new Color(60, 60, 60));
            }
        });
    }

    private void showPanel(String panelName) {
        cardLayout.show(contentPanel, panelName);
        if (panelName.equals("Movies")) {
            loadMovies();
        } else if (panelName.equals("History")) {
            loadBookingHistory("Mới nhất");
        }
    }

    private JPanel createMoviesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));

        JLabel titleLabel = new JLabel("Danh sách phim", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        movieListPanel = new JPanel();
        movieListPanel.setLayout(new BoxLayout(movieListPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(movieListPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Làm mới");
        refreshButton.addActionListener(e -> loadMovies());
        styleButton(refreshButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadMovies() {
        if (movieListPanel == null) return;

        JLabel loadingLabel = new JLabel("Đang tải danh sách phim...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));

        movieListPanel.removeAll();
        movieListPanel.add(loadingLabel);
        movieListPanel.revalidate();
        movieListPanel.repaint();

        new SwingWorker<List<Movie>, Void>() {
            @Override
            protected List<Movie> doInBackground() throws SQLException {
                try {
                    return movieBUS.getAllMovies();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return new ArrayList<>();
                }
            }
            @Override
            protected void done() {
                try {
                    List<Movie> movies = get();
                    movieListPanel.removeAll();
                    if (movies.isEmpty()) {
                        JLabel noMoviesLabel = new JLabel("Hiện không có phim nào.", SwingConstants.CENTER);
                        noMoviesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                        movieListPanel.add(noMoviesLabel);
                    } else {
                        for (Movie movie : movies) {
                            JPanel moviePanel = createMoviePanel(movie);
                            movieListPanel.add(moviePanel);
                            movieListPanel.add(Box.createVerticalStrut(10));
                        }
                    }
                    movieListPanel.revalidate();
                    movieListPanel.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            UserFrame.this,
                            "Không thể tải danh sách phim: " + ex.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    movieListPanel.removeAll();
                    JLabel errorLabel = new JLabel("Lỗi tải dữ liệu. Hãy thử lại.", SwingConstants.CENTER);
                    errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    movieListPanel.add(errorLabel);
                    movieListPanel.revalidate();
                    movieListPanel.repaint();
                }
            }
        }.execute();
    }

    private JPanel createMoviePanel(Movie movie) {
        JPanel moviePanel = new JPanel(new BorderLayout());
        moviePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        moviePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        moviePanel.setBackground(Color.WHITE);

        JPanel posterPanel = new JPanel(new BorderLayout());
        posterPanel.setPreferredSize(new Dimension(200, 140));
        posterPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JLabel posterLabel = new JLabel("Không có ảnh", SwingConstants.CENTER);
        posterLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        if (movie.getPoster() != null && !movie.getPoster().isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(movie.getPoster());
                if (icon.getIconWidth() > 0) {
                    posterLabel.setText("");
                    posterLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(180, 130, Image.SCALE_SMOOTH)));
                }
            } catch (Exception e) {
                System.err.println("Error loading poster for " + movie.getTitle() + ": " + e.getMessage());
            }
        }

        posterPanel.add(posterLabel, BorderLayout.CENTER);
        moviePanel.add(posterPanel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        infoPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(movie.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(5));

        JTextArea descriptionArea = new JTextArea();
        descriptionArea.setText(movie.getDescription());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(infoPanel.getBackground());
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        int rows = Math.min(3, descriptionArea.getText().split("\n").length);
        descriptionArea.setRows(rows);

        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        descScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScrollPane.setBorder(BorderFactory.createEmptyBorder());
        descScrollPane.setPreferredSize(new Dimension(600, 60));
        infoPanel.add(descScrollPane);
        infoPanel.add(Box.createVerticalStrut(10));

        JLabel roomLabel = new JLabel("Phòng chiếu: Đang tải...");
        roomLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        roomLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(roomLabel);
        infoPanel.add(Box.createVerticalStrut(5));

        JLabel priceLabel = new JLabel("Giá vé: Đang tải...");
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(priceLabel);
        infoPanel.add(Box.createVerticalStrut(10));

        JButton viewShowtimesButton = new JButton("Xem suất chiếu");
        viewShowtimesButton.setEnabled(false);
        viewShowtimesButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        viewShowtimesButton.setPreferredSize(new Dimension(120, 30));
        viewShowtimesButton.setMaximumSize(new Dimension(120, 30));

        viewShowtimesButton.setBackground(new Color(70, 130, 180));
        viewShowtimesButton.setForeground(Color.WHITE);
        viewShowtimesButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        viewShowtimesButton.setFocusPainted(false);
        viewShowtimesButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        viewShowtimesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        viewShowtimesButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                if (viewShowtimesButton.isEnabled()) {
                    viewShowtimesButton.setBackground(new Color(100, 149, 237));
                }
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                if (viewShowtimesButton.isEnabled()) {
                    viewShowtimesButton.setBackground(new Color(70, 130, 180));
                }
            }
        });

        infoPanel.add(viewShowtimesButton);
        moviePanel.add(infoPanel, BorderLayout.CENTER);

        loadRoomAndPriceInfo(movie, roomLabel, priceLabel, viewShowtimesButton);

        viewShowtimesButton.addActionListener(e -> showShowtimeDialog(movie));

        return moviePanel;
    }
    private void loadRoomAndPriceInfo(Movie movie, JLabel roomLabel, JLabel priceLabel, JButton viewShowtimesButton) {
        new SwingWorker<Room, Void>() {
            @Override
            protected Room doInBackground() {
                try {
                    List<Showtime> showtimes = showtimeBUS.getAllShowtimes().stream()
                            .filter(s -> s.getMovieID() == movie.getMovieID() && s.getIsVisible() == 0)
                            .collect(Collectors.toList());
                    if (!showtimes.isEmpty()) {
                        return roomBUS.getRoomById(showtimes.get(0).getRoomID());
                    }
                    return null;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
            @Override
            protected void done() {
                try {
                    Room movieRoom = get();
                    if (movieRoom != null) {
                        List<Showtime> showtimes = showtimeBUS.getAllShowtimes().stream()
                                .filter(s -> s.getRoomID() == movieRoom.getRoomID() && s.getMovieID() == movie.getMovieID() && s.getIsVisible() == 0)
                                .collect(Collectors.toList());
                        String showtimeText = showtimes.stream()
                                .map(s -> String.format("%s - %s",
                                        new SimpleDateFormat("HH:mm").format(s.getShowDate()),
                                        movieRoom.getRoomName()))
                                .collect(Collectors.joining(", "));
                        roomLabel.setText("Phòng chiếu: " + (showtimeText.isEmpty() ? "Không có" : showtimeText));
                        priceLabel.setText("Giá vé: " + String.format("%,.0f VND", movieRoom.getPrice()));
                        viewShowtimesButton.setEnabled(!showtimes.isEmpty());
                        viewShowtimesButton.setToolTipText(showtimes.isEmpty() ? "Phim chưa được xếp lịch chiếu" : "Xem suất chiếu");
                    } else {
                        roomLabel.setText("Phòng chiếu: Không có");
                        priceLabel.setText("Giá vé: Không có");
                        viewShowtimesButton.setEnabled(false);
                        viewShowtimesButton.setToolTipText("Phim chưa được xếp lịch chiếu");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    roomLabel.setText("Phòng chiếu: Lỗi tải dữ liệu");
                    priceLabel.setText("Giá vé: Lỗi tải dữ liệu");
                    viewShowtimesButton.setEnabled(false);
                }
            }
        }.execute();
    }

    private void openBookingFrame(Movie movie) {
        try {
            Room movieRoom = null;
            Showtime movieShowtime = null;
            List<Room> rooms = roomBUS.getAllRooms();
            List<Showtime> showtimes = showtimeBUS.getAllShowtimes();

            for (Room room : rooms) {
                if (room.getMovieTitle() != null && room.getMovieTitle().equals(movie.getTitle())) {
                    movieRoom = room;
                    movieShowtime = showtimes.stream()
                            .filter(s -> s.getRoomID() == room.getRoomID() &&
                                    s.getMovieID() == movie.getMovieID() &&
                                    s.getIsVisible() == 0)
                            .findFirst()
                            .orElse(null);
                    break;
                }
            }

            if (movieRoom != null && movieShowtime != null) {
                new BookingFrame(customerID, movieRoom.getRoomID(), movie.getMovieID(), movieShowtime.getShowtimeID()).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Phim này hiện không có phòng chiếu hoặc suất chiếu hợp lệ.",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể mở giao diện đặt vé: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));

        JLabel titleLabel = new JLabel("Lịch sử đặt vé", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        historyListPanel = new JPanel();
        historyListPanel.setLayout(new BoxLayout(historyListPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(historyListPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{"Mới nhất", "Cũ nhất", "Giá cao", "Giá thấp"});
        JButton refreshButton = new JButton("Làm mới");
        styleButton(refreshButton);

        controlPanel.add(new JLabel("Sắp xếp:"));
        controlPanel.add(sortCombo);
        controlPanel.add(refreshButton);
        panel.add(controlPanel, BorderLayout.SOUTH);

        sortCombo.addActionListener(e -> {
            if (sortCombo.getSelectedItem() != null) {
                loadBookingHistory(sortCombo.getSelectedItem().toString());
            }
        });

        refreshButton.addActionListener(e -> {
            if (sortCombo.getSelectedItem() != null) {
                loadBookingHistory(sortCombo.getSelectedItem().toString());
            }
        });

        return panel;
    }

    private void loadBookingHistory(String sortOption) {
        if (historyListPanel == null) return;

        JLabel loadingLabel = new JLabel("Đang tải lịch sử đặt vé...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));

        historyListPanel.removeAll();
        historyListPanel.add(loadingLabel);
        historyListPanel.revalidate();
        historyListPanel.repaint();

        new SwingWorker<List<BookingHistory>, Void>() {
            @Override
            protected List<BookingHistory> doInBackground() {
                try {
                    List<BookingHistory> historyList = ticketBUS.getBookingHistory(customerID);
                    switch (sortOption) {
                        case "Mới nhất":
                            historyList.sort((a, b) -> b.getBookingDate().compareTo(a.getBookingDate()));
                            break;
                        case "Cũ nhất":
                            historyList.sort((a, b) -> a.getBookingDate().compareTo(b.getBookingDate()));
                            break;
                        case "Giá cao":
                            historyList.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                            break;
                        case "Giá thấp":
                            historyList.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                            break;
                    }
                    return historyList;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return new ArrayList<>();
                }
            }
            @Override
            protected void done() {
                try {
                    List<BookingHistory> historyList = get();
                    historyListPanel.removeAll();
                    if (historyList.isEmpty()) {
                        JLabel noHistoryLabel = new JLabel("Bạn chưa có lịch sử đặt vé.", SwingConstants.CENTER);
                        noHistoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                        historyListPanel.add(noHistoryLabel);
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                        for (BookingHistory history : historyList) {
                            JPanel historyItem = createHistoryItem(history, sdf);
                            historyListPanel.add(historyItem);
                            historyListPanel.add(Box.createVerticalStrut(10));
                        }
                    }
                    historyListPanel.revalidate();
                    historyListPanel.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            UserFrame.this,
                            "Không thể tải lịch sử đặt vé: " + ex.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    historyListPanel.removeAll();
                    JLabel errorLabel = new JLabel("Lỗi tải dữ liệu. Hãy thử lại.", SwingConstants.CENTER);
                    errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    historyListPanel.add(errorLabel);
                    historyListPanel.revalidate();
                    historyListPanel.repaint();
                }
            }
        }.execute();
    }

    private JPanel createHistoryItem(BookingHistory history, SimpleDateFormat sdf) {
        JPanel historyItem = new JPanel();
        historyItem.setLayout(new BoxLayout(historyItem, BoxLayout.Y_AXIS));
        historyItem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        historyItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        historyItem.setBackground(Color.WHITE);

        JLabel movieLabel = new JLabel("Phim: " + history.getMovieTitle());
        movieLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        movieLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyItem.add(movieLabel);
        historyItem.add(Box.createVerticalStrut(5));

        JLabel roomLabel = new JLabel("Phòng: " + history.getRoomName());
        roomLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        roomLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyItem.add(roomLabel);
        historyItem.add(Box.createVerticalStrut(5));

        JLabel seatLabel = new JLabel("Ghế: " + history.getSeatNumber());
        seatLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        seatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyItem.add(seatLabel);
        historyItem.add(Box.createVerticalStrut(5));

        JLabel priceLabel = new JLabel(String.format("Giá vé: %,d VND", (int) history.getPrice()));
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyItem.add(priceLabel);
        historyItem.add(Box.createVerticalStrut(5));

        JLabel dateLabel = new JLabel("Ngày đặt: " + sdf.format(history.getBookingDate()));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyItem.add(dateLabel);

        return historyItem;
    }

    private void showShowtimeDialog(Movie movie) {
        JDialog dialog = new JDialog(this, "Suất chiếu của " + movie.getTitle(), true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel showtimePanel = new JPanel();
        showtimePanel.setLayout(new BoxLayout(showtimePanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(showtimePanel);
        dialog.add(scrollPane, BorderLayout.CENTER);

        new SwingWorker<List<Showtime>, Void>() {
            @Override
            protected List<Showtime> doInBackground() throws SQLException {
                try {
                    List<Showtime> allShowtimes = showtimeBUS.getAllShowtimes();
                    System.out.println("All Showtimes: " + allShowtimes); // Debug
                    List<Showtime> filteredShowtimes = allShowtimes.stream()
                            .filter(s -> s.getMovieID() == movie.getMovieID() && s.getIsVisible() == 0)
                            .toList();
                    System.out.println("Filtered Showtimes: " + filteredShowtimes); // Debug
                    return filteredShowtimes;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return new ArrayList<>();
                }
            }
            @Override
            protected void done() {
                try {
                    List<Showtime> showtimes = get();
                    showtimePanel.removeAll();
                    if (showtimes.isEmpty()) {
                        JLabel noShowtimesLabel = new JLabel("Không có suất chiếu khả dụng.", SwingConstants.CENTER);
                        noShowtimesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                        showtimePanel.add(noShowtimesLabel);
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        for (Showtime showtime : showtimes) {
                            JPanel showtimeItem = createShowtimeItem(showtime);
                            showtimePanel.add(showtimeItem);
                            showtimePanel.add(Box.createVerticalStrut(10));
                        }
                    }
                    showtimePanel.revalidate();
                    showtimePanel.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(dialog, "Không thể tải suất chiếu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    showtimePanel.removeAll();
                    JLabel errorLabel = new JLabel("Lỗi tải dữ liệu.", SwingConstants.CENTER);
                    errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    showtimePanel.add(errorLabel);
                    showtimePanel.revalidate();
                    showtimePanel.repaint();
                }
            }
        }.execute();

        dialog.setVisible(true);
    }

    private JPanel createShowtimeItem(Showtime showtime) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String showtimeInfo = String.format("%s - Phòng: %s - Trạng thái: %s",
                sdf.format(showtime.getShowDate()), showtime.getRoomName(), showtime.getStatus());
        JLabel label = new JLabel(showtimeInfo);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(label, BorderLayout.CENTER);

        JButton bookButton = new JButton("Đặt vé");
        bookButton.setPreferredSize(new Dimension(80, 25));
        bookButton.setBackground(new Color(70, 130, 180));
        bookButton.setForeground(Color.WHITE);
        bookButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bookButton.setFocusPainted(false);
        bookButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        bookButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        bookButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                bookButton.setBackground(new Color(100, 149, 237));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                bookButton.setBackground(new Color(70, 130, 180));
            }
        });

        bookButton.addActionListener(e -> {
            // Đóng dialog suất chiếu
            Window dialog = SwingUtilities.getWindowAncestor(bookButton);
            if (dialog != null) {
                dialog.dispose();
            }

            // Mở BookingFrame và đưa nó lên trước mặt
            BookingFrame bookingFrame = new BookingFrame(customerID, showtime.getRoomID(), showtime.getMovieID(), showtime.getShowtimeID());
            bookingFrame.setLocationRelativeTo(UserFrame.this); // Đặt vị trí so với UserFrame
            bookingFrame.setVisible(true);
            bookingFrame.toFront(); // Đưa frame lên trên cùng
            bookingFrame.requestFocus(); // Đảm bảo frame nhận focus
        });

        panel.add(bookButton, BorderLayout.EAST);

        return panel;
    }
}