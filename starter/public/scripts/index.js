// On website load, do the following
document.addEventListener("DOMContentLoaded", function () {
    // ----------------------------------------------------------------------
    // Set up HTML Elements

    // key: queue id, value: artist
    const queueMap = new Map();

    // Get ticket elements
    const ticketsSection = document.getElementById("ticket-section");
    const ticketElement = document.querySelector(".ticket-information");

    // Get Queue elements
    const queueSection = document.getElementById("queue-info");
    const queueElement = document.querySelector(".queue-element");
    const queueElementClone = queueElement.cloneNode(true);

    // remove the original ticket and queue element.
    ticketElement.style.display = "none";
    queueElement.remove();

    // Grab the selector element and attach an event listener
    const dropdown = document.getElementById("selector");

    dropdown.addEventListener("change", event => {
        if (event.target.value === "-1") {
            ticketElement.style.display = "none";
        } else {
            requestInfo(value);
        }
    })

    /**
     * Adds a new artist's concert to the dropdown.
     * @param concert
     */
    function appendToDropdown(concert) {
        const option = document.createElement("option");

        option.value = concert.id;
        option.text = `${concert.artist}: ${makeHumanReadable(concert.dateTime)}`;

        dropdown.add(option, null);
    }

    // Populate the dropdown with all the artists.
    fetch("/tickets", {
        method: "GET",
        headers: {"Accept": "application/json"}
    })
        .then((response) => {
            // handle the case that the response is an error.
            if (!response.ok) {
                // only a 500 error is possible for this request type
                if (response.status === 500) {
                    return response.text().then(text => {
                        throw new Error(text || "Server Error");
                    })
                }
                throw new Error(response.statusText);
            }

            // otherwise, the response was ok
            return response.json();
        })
        .then(data => {
            data.concerts.forEach(
                concert => {
                    appendToDropdown(concert);
                }
            )
        })
        .catch(err => {
            addTicketError(err.message || "Failed to retrieve resources");
        });

    // ----------------------------------------------------------------------
    // GET specific ticket info:

    /**
     * Sends a 'GET ticket/{id}' request to the server.
     * @param id The unique id of the artist.
     */
    function requestInfo(id) {
        fetch(`/tickets/${id}`, {
            method: "GET",
            headers: {"Accept": "application/json"}
        })
            .then((response) => {
                // handle the case that the response is an error.
                if (!response.ok) {
                    // only a 500 error is possible for this request type
                    if (response.status === 500) {
                        return response.text().then(text => {
                            throw new Error(text || "Server Error");
                        })
                    }
                    throw new Error(response.statusText);
                }

                // otherwise, the response was ok
                return response.json();
            })
            .then(concert => {
                addConcert(concert)
            })
    }

    /**
     * Generates a p element to show the client the error.
     * @param message The error message.
     */
    function addTicketError(message) {
        const errorElement = document.createElement("p");
        errorElement.textContent = message;

        ticketsSection.appendChild(errorElement);
    }

    /**
     * Creates a HTML element to represent a concert instance.
     * @param json The JSON object that contains the concert data.
     */
    function addConcert(json) {
        ticketElement.style.display = ""

        // add the text
        ticketElement.getElementsByClassName("artist")[0].textContent = json.artist;
        ticketElement.getElementsByClassName("venue")[0].textContent = json.venue;
        ticketElement.getElementsByClassName("datetime")[0].textContent = makeHumanReadable(json.dateTime);
        ticketElement.getElementsByClassName("count")[0].textContent = json.count;

        // Add the concert to the button and an associated queue event listener
        const button = ticketElement.getElementsByClassName("join-queue")[0];
        button.dataset.id = json.id;

        // event listener
        button.addEventListener("click", () => {
            let numberOfTickets = requestNumberOfTickets();
            if (numberOfTickets !== null) {
                queue(button.dataset.id, numberOfTickets);
            }
        })
    }

    /**
     * Reformats the data to be human-readable.
     * @param datetime The date as represented by a ISO 8601 UTC string
     * @returns {string} The reformatted date. Uses dd/mm/yyyy.
     */
    function makeHumanReadable(datetime) {
        let day = datetime.slice(8,10);
        let month = datetime.slice(5,7);
        let year = datetime.slice(0, 4);

        return day + "/" + month + "/" + year;
    }

    // ----------------------------------------------------------------------

    /**
     * Requests the server to queue a ticket purchase request.
     * @param artist The artist who's tickets the client wants to purchase.
     * @param numberOfTickets The number of tickets the client wants to purchase.
     */
    function queue(artist, numberOfTickets) {
        fetch(`/queue/${artist.replaceAll(" ", "-")}`, {
            method: "POST",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ tickets: numberOfTickets })
        })
            .then(res => {
                // 500 or 400 errors
                if (!res.ok) {
                    if (res.status === 400 || res.status === 500 ) {
                        // alert the user
                        return res.text().then(text => {
                            throw new Error(res.text || `Server error: ${res.status}`);
                        })
                    }
                    throw new Error(res.statusText);
                }

                return res.json()
            })
            .then(data => {
                window.alert(`Your request has been successfully added to the queue.\nYour Queue ID is: ${data.id}`);
                queueMap.set(data.id, artist);
            })
            .catch(error => {
                window.alert("Error: ", error);
            });
    }

    /**
     * Requests the user for the number of tickets they want to purchase.
     * @returns {number|null} The number of tickets the client wants to purchase.
     */
    function requestNumberOfTickets() {
        let res

        // prompt the user until the response is valid
        do {
            res = prompt("Please enter the number of tickets to be purchased: ");

            if (res === null) {
                return null;
            }

        } while (!Number.isInteger(Number(res)))

        return Number(res)
    }

    // periodic call to the reload the queue
    let intervalID = setInterval(updateQueue, 2000);

    /**
     * Updates the html elements in the queue section
     */
    function updateQueue() {
        if (queueMap.size === 0) {
            return;
        }

        // grabs the data for each queue element
        const fetchData = Array.from(queueMap.entries()).map(([queueId, artist]) => {
            return fetch(`/queue/${queueId}`, {
                method: "GET",
                headers: { "Accept": "application/json" }
            })
                .then(res => {
                    if (!res.ok) {
                        if (res.status === 404) {
                            return res.text().then(text => ({
                                position: -2,
                                errorMessage: text || "Queue not found",
                            }));
                        }
                        else if (res.status === 500) {
                            return res.text().then(text => ({
                                position: -2,
                                errorMessage: text || "Server Error",
                            }));
                        }
                    }

                    return res.json();
                })
                .then(data => {
                    // If purchase completed, update ticket count
                    if (data.position === -1 ) {
                        if (queueMap.has(data.id)) {
                            updateTicketCount(artist, data.tickets);
                            queueMap.delete(data.id);
                        }
                        return null; // and display nothing
                    }
                    return data;
                })
                .catch(error => {
                    return {
                        position: -2,
                        errorMessage: error.message || "Failed to retrieve queue information"
                    };
                });
        });

        Promise.all(fetchData).then(res => {
            document.querySelectorAll('.queue-element').forEach(e => e.remove());

            // Sort errors and valid results
            const validResults = res.filter(data => data && data.position >= 0);
            const errors = res.filter(data => data && data.position === -2);

            validResults.sort((a, b) => a.position - b.position);

            // display errors:
            if (errors.length > 0) {
                errors.forEach(error => {
                    addQueueError(error.errorMessage || "Failed to retrieve resources");
                })
            }

            // Add sorted queue elements
            validResults.forEach(data => {
                addQueueElement(data);
            });

        })
    }

    function addQueueError(message) {
        const errorElement = document.createElement("p");
        errorElement.textContent = message;
        errorElement.className = "queue-element";

        queueSection.appendChild(errorElement);
    }

    function updateTicketCount(artist, numberOfTickets) {
        const ticketElements = document.querySelectorAll(".ticket-information");

        ticketElements.forEach(element => {
           if (element.querySelector(".artist").textContent === artist) {
               const count = element.querySelector(".count");
               const currentCount = parseInt(count.textContent);

               count.textContent = currentCount - numberOfTickets;
           }
        });
    }

    function addQueueElement(data) {
        const clone = queueElementClone.cloneNode(true);
        clone.style.display = "";

        clone.getElementsByClassName("artist")[0].textContent = queueMap.get(data.id);
        clone.getElementsByClassName("position")[0].textContent = data.position;

        queueSection.appendChild(clone);
    }
})

